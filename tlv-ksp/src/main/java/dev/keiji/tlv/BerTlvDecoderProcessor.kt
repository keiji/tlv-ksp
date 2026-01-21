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

class BerTlvDecoderProcessor(
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

            processClass(classDeclaration, annotatedProperties, logger)
        }
    }

    @Suppress("LongMethod")
    private fun processClass(
        classDeclaration: KSClassDeclaration,
        annotatedProperties: Sequence<KSPropertyDeclaration>,
        logger: KSPLogger,
    ) {
        val packageName = classDeclaration.containingFile!!.packageName.asString()
        val className = "${classDeclaration.resolveNestedSimpleName("")}BerTlvDecoder"
        val file = codeGenerator.createNewFile(
            Dependencies(true, classDeclaration.containingFile!!),
            packageName,
            className
        )


        val importSet = resolveImportsForProperties(
            annotatedProperties,
            packageName,
            setOf("readFrom")
        )

        val imports = buildString {
            appendLine("import dev.keiji.tlv.BerTlvDecoder")
            appendLine("import java.io.*")
            appendLine("import java.math.BigInteger")
            importSet.sorted().forEach { appendLine("import $it") }
        }

        val targetQualifiedName = classDeclaration.requireQualifiedName().asString()

        val classTemplate0 = """
fun ${targetQualifiedName}.readFrom(
    byteArray: ByteArray,
    postCallback: BerTlvDecoder.Callback? = null,
) {
    readFrom(ByteArrayInputStream(byteArray), postCallback)
}
        """.trimIndent()

        val classTemplate1 = """
fun ${targetQualifiedName}.readFrom(
    inputStream: InputStream,
    postCallback: BerTlvDecoder.Callback? = null,
) {

    BerTlvDecoder.readFrom(inputStream,
        object : BerTlvDecoder.Callback {
            override fun onLargeItemDetected(
                tag: ByteArray,
                length: BigInteger,
                inputStream: InputStream
            ) {
                postCallback?.onLargeItemDetected(tag, length, inputStream)
            }

            override fun onUnknownLengthItemDetected(
                tag: ByteArray,
                inputStream: InputStream
            ) {
                postCallback?.onUnknownLengthItemDetected(tag, inputStream)
            }
        """.trimIndent()

        val classTemplate2 = """
        }
    )
}
        """.trimIndent()

        val onItemDetected = generateOnItemDetected(annotatedProperties, logger)

        file.use {
            it.appendText("package $packageName")
                .appendText("")
                .appendText(imports)
                .appendText("")
                .appendText(classTemplate0)
                .appendText("")
                .appendText(classTemplate1)
                .appendText("")
                .appendText(onItemDetected)
                .appendText("")
                .appendText(classTemplate2)
        }
    }

    @Suppress("MaxLineLength")
    private fun generateOnItemDetected(
        annotatedProperties: Sequence<KSPropertyDeclaration>,
        logger: KSPLogger,
    ): String {
        val sb = StringBuilder()

        val converterTable = HashMap<String, String>()
        val converters = annotatedProperties
            .map { prop -> getQualifiedName(prop, BerTlvItem::class, logger) }
            .distinct()
        converters.forEach { qualifiedName ->
            val variableName = generateVariableName(qualifiedName)
            sb.append("            val $variableName = ${qualifiedName}()\n")

            converterTable[qualifiedName] = variableName
        }

        sb.append("\n")

        sb.append("            override fun onItemDetected(tag: ByteArray, value: ByteArray) {\n")
        sb.append("                if (false) {\n")
        sb.append("                    // Do nothing\n")

        annotatedProperties.forEach { prop ->
            val tagArray = getTagArrayAsString(prop, BerTlvItem::class, logger)
            val qualifiedName = getQualifiedName(prop, BerTlvItem::class, logger)
            val converterVariableName = converterTable[qualifiedName]
            sb.append("                } else if (byteArrayOf(${tagArray}).contentEquals(tag)) {\n")

            val decClass = prop.type.resolve().declaration as KSClassDeclaration
            if (berTlvClasses.contains(decClass)) {
                val className = decClass.requireQualifiedName().asString()
                sb.append("                    this@readFrom.${prop.simpleName.asString()} = ${className}().also { it.readFrom(value) }\n")
            } else {
                sb.append("                    this@readFrom.${prop.simpleName.asString()} = ${converterVariableName}.convertFromByteArray(value)\n")
            }
        }

        sb.append("                } else {\n")
        sb.append("                    // Do nothing\n")
        sb.append("                }\n")
        sb.append("                postCallback?.onItemDetected(tag, value)\n")
        sb.append("            }\n")
        return sb.toString()
    }
}
