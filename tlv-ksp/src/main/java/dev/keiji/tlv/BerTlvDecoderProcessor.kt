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

class BerTlvDecoderProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {

    private lateinit var berTlvClasses: Sequence<KSAnnotated>
    private lateinit var berTlvClassesStringList: List<String>

    override fun process(resolver: Resolver): List<KSAnnotated> {
        berTlvClasses = resolver.getSymbolsWithAnnotation(BerTlv::class.qualifiedName!!)
        val ret = berTlvClasses.filter { !it.validate() }.toList()

        berTlvClassesStringList = berTlvClasses
            .filter { it is KSClassDeclaration && it.validate() }
            .map { it as KSClassDeclaration }
            .mapNotNull { it.qualifiedName?.asString() }
            .toList()

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
                    prop.annotations.any { anno -> isTargetAnnotation(anno) }
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
fun ${classDeclaration.simpleName.asString()}.readFrom(data: ByteArray) {

    BerTlvDecoder.readFrom(ByteArrayInputStream(data),
        object : BerTlvDecoder.Companion.Callback {
            override fun onLargeItemDetected(
                tag: ByteArray,
                length: BigInteger,
                inputStream: InputStream
            ) {
                throw StreamCorruptedException("tag length is too large.")
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

        sb.append("            override fun onItemDetected(tag: ByteArray, data: ByteArray) {\n")
        sb.append("                if (false) {\n")
        sb.append("                    // Do nothing\n")

        annotatedProperties.forEach { prop ->
            val annotationName = getAnnotationName(prop, logger)
            val tag = getTagAsString(prop, logger)
            val qualifiedName = getQualifiedName(prop, logger)
            val converterVariableName = converterTable[qualifiedName]

            val className = prop.type.resolve().declaration.qualifiedName?.asString()

            sb.append("                } else if (${tag}.contentEquals(tag)) {\n")

            if (annotationName == BerTlvItem::class.simpleName) {
                if (berTlvClassesStringList.contains(className)) {
                    sb.append("                    this@readFrom.${prop.simpleName.asString()} = ${className}().also { it.readFrom(data) }\n")
                } else {
                    sb.append("                    this@readFrom.${prop.simpleName.asString()} = ${converterVariableName}.convertFromByteArray(data)\n")
                }
            } else if (annotationName == BerTlvItemList::class.simpleName) {
                val genericClassName = getGenericsTypeNameFromList(prop)

                sb.append("                    val list = this@readFrom.${prop.simpleName.asString()} ?: ${className}()\n")
                if (berTlvClassesStringList.contains(genericClassName)) {
                    sb.append("                    list.add(${genericClassName}().also { it.readFrom(data) })\n")
                } else {
                    sb.append("                    list.add(${converterVariableName}.convertFromByteArray(data))\n")
                }
                sb.append("                    this@readFrom.${prop.simpleName.asString()} = list\n")
            }
        }

        sb.append("                } else {\n")
        sb.append("                    // Do nothing\n")
        sb.append("                }\n")
        sb.append("            }\n")
        return sb.toString()
    }
}
