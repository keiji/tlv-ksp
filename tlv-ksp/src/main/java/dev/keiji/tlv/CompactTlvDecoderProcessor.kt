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

class CompactTlvDecoderProcessor(
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

            processClass(classDeclaration, annotatedProperties, logger)
        }
    }

    private fun processClass(
        classDeclaration: KSClassDeclaration,
        annotatedProperties: Sequence<KSPropertyDeclaration>,
        logger: KSPLogger,
    ) {
        val packageName = classDeclaration.containingFile!!.packageName.asString()
        val className = "${classDeclaration.simpleName.asString()}CompactTlvDecoder"

        val imports = """
import dev.keiji.tlv.CompactTlvDecoder
import java.io.*
        """.trimIndent()

        val classTemplate0 = """
fun ${classDeclaration.simpleName.asString()}.readFrom(
    byteArray: ByteArray,
    postCallback: CompactTlvDecoder.Callback? = null,
) {
    readFrom(ByteArrayInputStream(byteArray), postCallback)
}
        """.trimIndent()

        val classTemplate1 = """
fun ${classDeclaration.simpleName.asString()}.readFrom(
    inputStream: InputStream,
    postCallback: CompactTlvDecoder.Callback? = null,
) {

    CompactTlvDecoder.readFrom(inputStream,
        object : CompactTlvDecoder.Callback {
        """.trimIndent()

        val classTemplate2 = """
        }
    )
}
        """.trimIndent()

        val onItemDetected = generateOnItemDetected(annotatedProperties, logger)

        codeGenerator.createNewFile(
            Dependencies(true, classDeclaration.containingFile!!),
            packageName,
            className
        ).use {
            it.appendLine("package $packageName")
                .appendLine("")
                .appendLine(imports)
                .appendLine("")
                .appendLine(classTemplate0)
                .appendLine("")
                .appendLine(classTemplate1)
                .appendLine("")
                .appendLine(onItemDetected)
                .appendLine("")
                .appendLine(classTemplate2)
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
            .map { prop -> getQualifiedName(prop, CompactTlvItem::class, logger) }
            .distinct()
        converters.forEach { qualifiedName ->
            val variableName = generateVariableName(qualifiedName)
            sb.append("            val $variableName = ${qualifiedName}()\n")

            converterTable[qualifiedName] = variableName
        }

        sb.append("\n")

        sb.append("            override fun onItemDetected(tag: Byte, value: ByteArray) {\n")
        sb.append("                if (false) {\n")
        sb.append("                    // Do nothing\n")

        annotatedProperties.forEach { prop ->
            val tag = getTagAsString(prop, CompactTlvItem::class, logger)
            val qualifiedName = getQualifiedName(prop, CompactTlvItem::class, logger)
            val converterVariableName = converterTable[qualifiedName]
            sb.append("                } else if (${tag} == tag) {\n")

            val decClass = prop.type.resolve().declaration
            if (compactTlvClasses.contains(decClass)) {
                val className = decClass.simpleName.asString()
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
