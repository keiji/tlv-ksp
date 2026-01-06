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
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import java.io.ByteArrayOutputStream
import java.io.OutputStream

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

            val fileSpec = FileSpec.builder(packageName, className)
                .addFunction(generateWriteToFunction(classDeclaration, annotatedProperties, logger))
                .build()

            fileSpec.writeTo(codeGenerator, Dependencies(true, classDeclaration.containingFile!!))
        }

        private fun generateWriteToFunction(
            classDeclaration: KSClassDeclaration,
            annotatedProperties: Sequence<KSPropertyDeclaration>,
            logger: KSPLogger
        ): FunSpec {
            val receiverType = classDeclaration.toClassName()

            return FunSpec.builder("writeTo")
                .receiver(receiverType)
                .addParameter("outputStream", OutputStream::class)
                .addCode(generateWriteToCode(annotatedProperties, logger))
                .build()
        }

        @Suppress("MaxLineLength")
        private fun generateWriteToCode(
            annotatedProperties: Sequence<KSPropertyDeclaration>,
            logger: KSPLogger
        ): CodeBlock {
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

            annotatedProperties.forEach { prop ->
                val tag = getTagAsString(prop, CompactTlvItem::class, logger)
                val qualifiedName = getQualifiedName(prop, CompactTlvItem::class, logger)
                val converterVariableName = converterTable[qualifiedName]

                val isNullable = prop.type.resolve().isMarkedNullable
                val propName = prop.simpleName.asString()
                val propAccess = if (isNullable) "$propName?" else propName

                val decClass = prop.type.resolve().declaration
                val compactTlvEncoderClass = ClassName("dev.keiji.tlv", "CompactTlvEncoder")

                if (compactTlvClasses.contains(decClass)) {
                    block.beginControlFlow("%L.also", propAccess)
                    block.addStatement("val data = %T().let { baos ->", ByteArrayOutputStream::class)
                    block.indent()

                    if (isNullable) {
                        block.addStatement("it.writeTo(baos)")
                    } else {
                        block.addStatement("this.%N.writeTo(baos)", propName)
                    }

                    block.addStatement("baos.toByteArray()")
                    block.unindent()
                    block.addStatement("}") // close let

                    block.addStatement("%T.writeTo(%L, data, outputStream)", compactTlvEncoderClass, tag)
                    block.endControlFlow() // also
                } else {
                    block.beginControlFlow("%L.also", propAccess)
                    block.addStatement(
                        "%T.writeTo(%L, %N.convertToByteArray(it), outputStream)",
                        compactTlvEncoderClass,
                        tag,
                        converterVariableName
                    )
                    block.endControlFlow()
                }
            }

            return block.build()
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
