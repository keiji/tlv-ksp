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
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import java.lang.StringBuilder
import kotlin.collections.HashMap

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

    private fun processClass(
        classDeclaration: KSClassDeclaration,
        annotatedProperties: Sequence<KSPropertyDeclaration>,
        logger: KSPLogger,
    ) {
        val packageName = classDeclaration.containingFile!!.packageName.asString()
        val className = "${classDeclaration.simpleName.asString()}BerTlvDecoder"
        val file = codeGenerator.createNewFile(
            Dependencies(true, classDeclaration.containingFile!!),
            packageName,
            className
        )

        val imports = """
import dev.keiji.tlv.BerTlvDecoder
import java.io.*
import java.math.BigInteger
        """.trimIndent()

        val classTemplate1 = """
fun ${classDeclaration.simpleName.asString()}.readFrom(
    data: ByteArray,
    postCallback: BerTlvDecoder.Callback? = null,
) {

    BerTlvDecoder.readFrom(ByteArrayInputStream(data),
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

        file.appendText("package $packageName")
            .appendText("")
            .appendText(imports)
            .appendText("")
            .appendText(classTemplate1)
            .appendText("")
            .appendText(onItemDetected)
            .appendText("")
            .appendText(classTemplate2)
    }

    private fun generateOnItemDetected(
        annotatedProperties: Sequence<KSPropertyDeclaration>,
        logger: KSPLogger,
    ): String {
        val sb = StringBuilder()

        val converterTable = HashMap<String, String>()
        val converters = annotatedProperties
            .map { prop -> getQualifiedName(prop, logger) }
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
            val tag = getTagAsString(prop, logger)
            val qualifiedName = getQualifiedName(prop, logger)
            val converterVariableName = converterTable[qualifiedName]
            sb.append("                } else if (${tag}.contentEquals(tag)) {\n")

            val decClass = prop.type.resolve().declaration
            if (berTlvClasses.contains(decClass)) {
                val className = decClass.simpleName.asString()
                sb.append("                    this@readFrom.${prop.simpleName.asString()} = ${className}().also { it.readFrom(data) }\n")
            } else {
                sb.append("                    this@readFrom.${prop.simpleName.asString()} = ${converterVariableName}.convertFromByteArray(data)\n")
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
