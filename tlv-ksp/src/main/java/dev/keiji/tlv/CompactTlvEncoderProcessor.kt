/*
 * Copyright (C) 2022 ARIYAMA Keiji
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.keiji.tlv

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate

private const val MAX_TAG_VALUE: Byte = 0b00001111
private const val ZERO: Byte = 0

class CompactTlvEncoderProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {
    private lateinit var compactTlvClasses: Sequence<KSAnnotated>

    override fun process(resolver: Resolver): List<KSAnnotated> {
        compactTlvClasses = resolver.getSymbolsWithAnnotation(CompactTlv::class.qualifiedName!!)
        val ret = compactTlvClasses.filter { !it.validate() }.toList()

        compactTlvClasses
            .filter { it is KSClassDeclaration && it.validate() }
            .forEach { it.accept(CompactTlvVisitor(), Unit) }

        return ret
    }

    private inner class CompactTlvVisitor : KSVisitorVoid() {

        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            super.visitClassDeclaration(classDeclaration, data)

            classDeclaration.primaryConstructor!!.accept(this, data)

            val annotatedProperties = classDeclaration.getAllProperties()
                .filter { it.validate() }
                .filter { prop ->
                    prop.annotations.any { anno ->
                        anno.shortName.asString() == CompactTlvItem::class.simpleName
                    }
                }

            validateAnnotations(annotatedProperties, logger)

            processClass(
                classDeclaration,
                annotatedProperties.sortedWith(annotatedPropertyOrderComparator),
                logger
            )
        }

        private val annotatedPropertyOrderComparator =
            Comparator<KSPropertyDeclaration> { obj1, obj2 ->
                if (obj1 === obj2) {
                    return@Comparator 0
                }

                val obj1Order = getOrder(obj1, CompactTlvItem::class)
                val obj2Order = getOrder(obj2, CompactTlvItem::class)

                return@Comparator obj1Order.compareTo(obj2Order)
            }

        private fun validateAnnotations(
            annotatedProperties: Sequence<KSPropertyDeclaration>,
            logger: KSPLogger,
        ) {
            annotatedProperties.forEach { prop ->
                val className = prop.parent.toString()
                val propertyName = prop.simpleName.asString()
                val tag = getTagAsByte(prop, CompactTlvItem::class)
                validateAnnotation(tag, className, propertyName, logger)
            }
        }

        @Suppress("UnusedParameter")
        private fun processClass(
            classDeclaration: KSClassDeclaration,
            annotatedProperties: Sequence<KSPropertyDeclaration>,
            logger: KSPLogger,
        ) {
            val packageName = classDeclaration.containingFile!!.packageName.asString()
            val className = "${classDeclaration.simpleName.asString()}CompactTlvEncoder"
            val file = codeGenerator.createNewFile(
                Dependencies(true, classDeclaration.containingFile!!),
                packageName,
                className
            )

            val importSet = resolveImportsForProperties(
                annotatedProperties,
                packageName,
                setOf("writeTo")
            )

            val imports = buildString {
                appendLine("import dev.keiji.tlv.CompactTlvEncoder")
                appendLine("import java.io.*")
                importSet.sorted().forEach { appendLine("import $it") }
            }

            val targetQualifiedName = classDeclaration.requireQualifiedName().asString()

            val classTemplate1 = """
fun ${targetQualifiedName}.writeTo(outputStream: OutputStream) {
        """.trimIndent()

            val classTemplate2 = """
}
        """.trimIndent()

            val writeTo = generateWriteTo(annotatedProperties)

            file.use {
                it.appendText("package $packageName")
                    .appendText(imports)
                    .appendText("")
                    .appendText(classTemplate1)
                    .appendText(writeTo)
                    .appendText(classTemplate2)
            }
        }

        @Suppress("MaxLineLength")
        private fun generateWriteTo(
            annotatedProperties: Sequence<KSPropertyDeclaration>
        ): String {
            val sb = StringBuilder()

            val converterTable = HashMap<String, String>()
            val converters = annotatedProperties
                .map { prop -> getQualifiedName(prop, CompactTlvItem::class, logger) }
                .distinct()
            converters.forEach { qualifiedName ->
                val variableName = generateVariableName(qualifiedName)
                sb.append("    val $variableName = ${qualifiedName}()\n")

                converterTable[qualifiedName] = variableName
            }

            sb.append("\n")

            annotatedProperties.forEach { prop ->
                val tag = getTagAsString(prop, CompactTlvItem::class, logger)
                val qualifiedName = getQualifiedName(prop, CompactTlvItem::class, logger)
                val converterVariableName = converterTable[qualifiedName]
                val propName =
                    prop.simpleName.asString() + if (prop.type.resolve().isMarkedNullable) "?" else ""

                val decClass = prop.type.resolve().declaration
                if (compactTlvClasses.contains(decClass)) {
                    sb.append("    ${propName}.also {\n")
                    sb.append("        val data = ByteArrayOutputStream().let { baos ->\n")
                    sb.append("            ${propName}.writeTo(baos)\n")
                    sb.append("            baos.toByteArray()\n")
                    sb.append("        }\n")
                    sb.append("        CompactTlvEncoder.writeTo(${tag}, data, outputStream)\n")
                    sb.append("    }\n")
                } else {
                    sb.append("    ${propName}.also {\n")
                    sb.append("        CompactTlvEncoder.writeTo(${tag}, ${converterVariableName}.convertToByteArray(it), outputStream)\n")
                    sb.append("    }\n")
                }
            }

            return sb.toString()
        }
    }

    companion object {
        @Suppress("UnusedParameter")
        internal fun validateAnnotation(
            tag: Byte,
            className: String = "",
            propertyName: String = "",
            logger: KSPLogger? = null,
        ) {
            if (tag > MAX_TAG_VALUE || tag < ZERO) {
                val lead = lead(className, propertyName)
                throw IllegalArgumentException(
                    "$lead tag ${tag.toHex()} must be less or equals ${MAX_TAG_VALUE.toHex()}."
                )
            }
        }
    }
}
