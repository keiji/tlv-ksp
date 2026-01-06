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

private const val MASK_MSB_BITS = 0b100_00000
private const val MASK_TAG_BITS = 0b00_0_11111

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

        private val annotatedPropertyOrderComparator =
            Comparator<KSPropertyDeclaration> { obj1, obj2 ->
                if (obj1 === obj2) {
                    return@Comparator 0
                }

                val obj1Order = getOrder(obj1, BerTlvItem::class)
                val obj2Order = getOrder(obj2, BerTlvItem::class)

                return@Comparator obj1Order.compareTo(obj2Order)
            }

        private fun validateAnnotations(
            annotatedProperties: Sequence<KSPropertyDeclaration>,
            logger: KSPLogger,
        ) {
            annotatedProperties.forEach { prop ->
                val className = prop.parent.toString()
                val propertyName = prop.simpleName.asString()
                val tagArray = getTagAsByteArray(prop, BerTlvItem::class)
                validateAnnotation(tagArray, className, propertyName, logger)
            }
        }

        @Suppress(
            "UnusedParameter",
            "MaxLineLength",
        )
        private fun processClass(
            classDeclaration: KSClassDeclaration,
            annotatedProperties: Sequence<KSPropertyDeclaration>,
            logger: KSPLogger,
        ) {
            val packageName = classDeclaration.containingFile!!.packageName.asString()
            val className = "${classDeclaration.simpleName.asString()}BerTlvEncoder"

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
                .map { prop -> getQualifiedName(prop, BerTlvItem::class, logger) }
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
                val tagArray = getTagArrayAsString(prop, BerTlvItem::class, logger)
                val qualifiedName = getQualifiedName(prop, BerTlvItem::class, logger)
                val longDefLengthFieldSizeAtLeast =
                    getLongDefLengthFieldSizeAtLeast(prop, BerTlvItem::class, logger)
                val converterVariableName = converterTable[qualifiedName]

                val isNullable = prop.type.resolve().isMarkedNullable
                val propName = prop.simpleName.asString()
                val propAccess = if (isNullable) "$propName?" else propName

                val decClass = prop.type.resolve().declaration
                val berTlvEncoderClass = ClassName("dev.keiji.tlv", "BerTlvEncoder")

                if (berTlvClasses.contains(decClass)) {
                    block.beginControlFlow("%L.also", propAccess)
                    block.addStatement("val data = %T().let { baos ->", ByteArrayOutputStream::class)

                    // Inside also, `it` is the value.
                    // If isNullable, `it` is non-null because `?.also` calls only if non-null.
                    // The original code used property name recursively on the object?
                    // Wait, original: ${propName}.writeTo(baos)
                    // If propName is "obj?", then "obj?.writeTo(baos)".
                    // But inside also { ... }, if we use `it`, it is cleaner.
                    // However, `writeTo` is an extension function we are generating or exists.
                    // Let's assume we can use `it.writeTo(baos)`.

                    if (isNullable) {
                        block.addStatement("it.writeTo(baos)")
                    } else {
                        block.addStatement("this.%N.writeTo(baos)", propName)
                    }

                    block.addStatement("baos.toByteArray()")
                    block.endControlFlow() // let

                    block.addStatement("%T.writeTo(byteArrayOf(%L), data, outputStream, longDefLengthFieldSizeAtLeast = %L)",
                        berTlvEncoderClass,
                        tagArray,
                        longDefLengthFieldSizeAtLeast
                    )
                    block.endControlFlow() // also
                } else {
                    block.beginControlFlow("%L.also", propAccess)
                    block.addStatement(
                        "%T.writeTo(byteArrayOf(%L), %N.convertToByteArray(it), outputStream, longDefLengthFieldSizeAtLeast = %L)",
                        berTlvEncoderClass,
                        tagArray,
                        converterVariableName,
                        longDefLengthFieldSizeAtLeast
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
            tag: ByteArray,
            className: String = "",
            propertyName: String = "",
            logger: KSPLogger? = null,
        ) {

            @Suppress("MagicNumber")
            val firstByte: Int = tag.first().toInt() and 0xFF

            if ((firstByte and MASK_TAG_BITS) != MASK_TAG_BITS && tag.size > 1) {
                val lead = lead(className, propertyName)
                throw IllegalArgumentException(
                    "$lead tag ${tag.toHex(":")} seems to short(1 byte) definition." +
                            " However, it seems to long definition expectedly."
                )
            } else if ((firstByte and MASK_TAG_BITS) == MASK_TAG_BITS && tag.size < 2) {
                val lead = lead(className, propertyName)
                throw IllegalArgumentException(
                    "$lead tag ${tag.toHex(":")} seems to long(n bytes) definition." +
                            " However, it seems to short definition expectedly."
                )
            }

            tag.forEachIndexed { index, b ->
                // Skip index 0
                if (index == 0) {
                    return@forEachIndexed
                }

                @Suppress("MagicNumber")
                val value = b.toInt() and 0xFF

                // Check lastIndex(= tag.size - 1)
                if (index == (tag.size - 1)) {
                    if ((value and MASK_MSB_BITS) == MASK_MSB_BITS) {
                        val lead = lead(className, propertyName)
                        throw IllegalArgumentException(
                            "$lead tag ${tag.toHex(":")} seems to be long(n bytes) definition." +
                                    " last element ${tag[index].toHex()} seems to be continued."
                        )
                    }
                    return@forEachIndexed
                }

                // index 1 to (lastIndex - 1)
                if ((value and MASK_MSB_BITS) != MASK_MSB_BITS) {
                    val lead = lead(className, propertyName)
                    throw IllegalArgumentException(
                        "$lead tag ${tag.toHex(":")} seems to be long(n bytes) definition." +
                                " index $index element ${tag[index].toHex()} MSB must be true."
                    )
                }
            }
        }
    }
}
