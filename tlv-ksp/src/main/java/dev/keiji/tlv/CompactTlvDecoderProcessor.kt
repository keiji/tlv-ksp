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
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import java.io.ByteArrayInputStream
import java.io.InputStream

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

        val fileSpec = FileSpec.builder(packageName, className)
            .addFunction(generateReadFromByteArray(classDeclaration))
            .addFunction(generateReadFromInputStream(classDeclaration, annotatedProperties, logger))
            .build()

        fileSpec.writeTo(codeGenerator, Dependencies(true, classDeclaration.containingFile!!))
    }

    private fun generateReadFromByteArray(
        classDeclaration: KSClassDeclaration
    ): FunSpec {
        val compactTlvDecoderCallback = ClassName("dev.keiji.tlv", "CompactTlvDecoder", "Callback")
        val receiverType = classDeclaration.toClassName()

        return FunSpec.builder("readFrom")
            .receiver(receiverType)
            .addParameter("byteArray", ByteArray::class)
            .addParameter(
                ParameterSpec.builder("postCallback", compactTlvDecoderCallback.copy(nullable = true))
                    .defaultValue("null")
                    .build()
            )
            .addStatement("readFrom(%T(byteArray), postCallback)", ByteArrayInputStream::class)
            .build()
    }

    private fun generateReadFromInputStream(
        classDeclaration: KSClassDeclaration,
        annotatedProperties: Sequence<KSPropertyDeclaration>,
        logger: KSPLogger
    ): FunSpec {
        val compactTlvDecoder = ClassName("dev.keiji.tlv", "CompactTlvDecoder")
        val compactTlvDecoderCallback = compactTlvDecoder.nestedClass("Callback")
        val receiverType = classDeclaration.toClassName()

        val callbackObject = TypeSpec.anonymousClassBuilder()
            .addSuperinterface(compactTlvDecoderCallback)
            .addFunction(generateOnItemDetected(classDeclaration, annotatedProperties, logger))
            .build()

        return FunSpec.builder("readFrom")
            .receiver(receiverType)
            .addParameter("inputStream", InputStream::class)
            .addParameter(
                ParameterSpec.builder("postCallback", compactTlvDecoderCallback.copy(nullable = true))
                    .defaultValue("null")
                    .build()
            )
            .addStatement("%T.readFrom(inputStream, %L)", compactTlvDecoder, callbackObject)
            .build()
    }

    @Suppress("UnusedParameter", "MaxLineLength")
    private fun generateOnItemDetected(
        classDeclaration: KSClassDeclaration,
        annotatedProperties: Sequence<KSPropertyDeclaration>,
        logger: KSPLogger,
    ): FunSpec {
        val funcBuilder = FunSpec.builder("onItemDetected")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("tag", Byte::class)
            .addParameter("value", ByteArray::class)

        val block = CodeBlock.builder()

        val converterTable = HashMap<String, String>()
        val converters = annotatedProperties
            .map { prop -> getQualifiedName(prop, CompactTlvItem::class, logger) }
            .distinct()

        converters.forEach { qualifiedName ->
            val variableName = generateVariableName(qualifiedName)
            block.addStatement("val %N = %T()", variableName, ClassName.bestGuess(qualifiedName))
            converterTable[qualifiedName] = variableName
        }

        if (converters.iterator().hasNext()) {
            block.add("\n")
        }

        block.beginControlFlow("if (false)")
        block.addStatement("// Do nothing")

        annotatedProperties.forEach { prop ->
            val tag = getTagAsString(prop, CompactTlvItem::class, logger)
            val qualifiedName = getQualifiedName(prop, CompactTlvItem::class, logger)
            val converterVariableName = converterTable[qualifiedName]

            block.nextControlFlow("else if (%L == tag)", tag)

            val decClass = prop.type.resolve().declaration
            if (compactTlvClasses.contains(decClass)) {
                val className = (decClass as KSClassDeclaration).toClassName()
                block.addStatement("this@readFrom.%N = %T().also { it.readFrom(value) }", prop.simpleName.asString(), className)
            } else {
                block.addStatement("this@readFrom.%N = %N.convertFromByteArray(value)", prop.simpleName.asString(), converterVariableName)
            }
        }

        block.nextControlFlow("else")
        block.addStatement("// Do nothing")
        block.endControlFlow()

        block.addStatement("postCallback?.onItemDetected(tag, value)")

        return funcBuilder.addCode(block.build()).build()
    }
}
