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

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import java.lang.StringBuilder

class BerTlvEncoderProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {
    private lateinit var berTlvClasses: Sequence<KSAnnotated>

    override fun process(resolver: Resolver): List<KSAnnotated> {
        berTlvClasses = resolver.getSymbolsWithAnnotation(BerTlv::class.qualifiedName!!)
        val ret = berTlvClasses.filter { !it.validate() }.toList()

        berTlvClasses
            .filter { it is KSClassDeclaration && it.validate() }
            .forEach { it.accept(BerTlvVisitor(), Unit) }

        return ret
    }

    private inner class BerTlvVisitor : KSVisitorVoid() {

        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            super.visitClassDeclaration(classDeclaration, data)

            classDeclaration.primaryConstructor!!.accept(this, data)

            val annotatedProperties = classDeclaration.getAllProperties()
                .filter { it.validate() }
                .filter { prop ->
                    prop.annotations.any { anno ->
                        anno.shortName.asString() == BerTlvItem::class.simpleName
                    }
                }

            validateAnnotations(annotatedProperties, logger)

            processClass(
                classDeclaration,
                annotatedProperties.sortedWith(annotatedPropertyOrderComparator),
                logger
            )
        }

        private fun validateAnnotations(
            annotatedProperties: Sequence<KSPropertyDeclaration>,
            logger: KSPLogger,
        ) {
            annotatedProperties.forEach { prop ->
                val className = prop.parent.toString()
                val propertyName = prop.simpleName.asString()
                val tagArray = getTagAsByteArray(prop)
                validateAnnotation(tagArray, className, propertyName, logger)
            }
        }

        private fun processClass(
            classDeclaration: KSClassDeclaration,
            annotatedProperties: Sequence<KSPropertyDeclaration>,
            logger: KSPLogger,
        ) {
            val packageName = classDeclaration.containingFile!!.packageName.asString()
            val className = "${classDeclaration.simpleName.asString()}BerTlvEncoder"
            val file = codeGenerator.createNewFile(
                Dependencies(true, classDeclaration.containingFile!!),
                packageName,
                className
            )

            val imports = """
import dev.keiji.tlv.BerTlvEncoder
import java.io.*
        """.trimIndent()

            val classTemplate1 = """
fun ${classDeclaration.simpleName.asString()}.writeTo(outputStream: OutputStream) {
        """.trimIndent()

            val classTemplate2 = """
}
        """.trimIndent()

            val writeTo = generateWriteTo(annotatedProperties)

            file.appendText("package $packageName")
                .appendText("")
                .appendText(imports)
                .appendText("")
                .appendText(classTemplate1)
                .appendText(writeTo)
                .appendText(classTemplate2)
        }

        private fun generateWriteTo(
            annotatedProperties: Sequence<KSPropertyDeclaration>
        ): String {
            val sb = StringBuilder()

            val converterTable = HashMap<String, String>()
            val converterPairs = annotatedProperties
                .map { prop -> getConverterAsString(prop, logger) }
                .distinct()
            converterPairs.forEach { converterPair ->
                val (packageName, qualifiedName) = converterPair
                val variableName = stripPackage(packageName, qualifiedName)
                sb.append("    val $variableName = ${qualifiedName}()\n")

                converterTable[qualifiedName] = variableName
            }

            sb.append("\n")

            annotatedProperties.forEach { prop ->
                val tag = getTagAsString(prop, logger)
                val (_, qualifiedName) = getConverterAsString(prop, logger)
                val converterVariableName = converterTable[qualifiedName]
                val propName =
                    prop.simpleName.asString() + if (prop.type.resolve().isMarkedNullable) "?" else ""

                val decClass = prop.type.resolve().declaration
                if (berTlvClasses.contains(decClass)) {
                    sb.append("    ${propName}.also {\n")
                    sb.append("        val data = ByteArrayOutputStream().let { baos ->\n")
                    sb.append("            ${propName}.writeTo(baos)\n")
                    sb.append("            baos.toByteArray()\n")
                    sb.append("        }\n")
                    sb.append("        BerTlvEncoder.writeTo(${tag}, data, outputStream)\n")
                    sb.append("    }\n")
                } else {
                    sb.append("    ${propName}.also {\n")
                    sb.append("        BerTlvEncoder.writeTo(${tag}, ${converterVariableName}.convertToByteArray(it), outputStream)\n")
                    sb.append("    }\n")
                }
            }

            return sb.toString()
        }
    }
}
