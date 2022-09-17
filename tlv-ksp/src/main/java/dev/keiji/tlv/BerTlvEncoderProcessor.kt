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

    companion object {
        private const val MASK_MSB_BITS = 0b100_00000
        private const val MASK_TAG_BITS = 0b00_0_11111

        private fun lead(
            className: String,
            propertyName: String
        ): String {
            return if (className.isNotEmpty() && propertyName.isNotEmpty()) {
                "Class $className property $propertyName"
            } else {
                ""
            }
        }

        internal fun validateAnnotation(
            tag: ByteArray,
            className: String = "",
            propertyName: String = ""
        ) {
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

            validateAnnotations(annotatedProperties)
            processClass(classDeclaration, annotatedProperties)
        }

        private fun validateAnnotations(annotatedProperties: Sequence<KSPropertyDeclaration>) {
            annotatedProperties.forEach { prop ->
                val className = prop.parent.toString()
                val propertyName = prop.simpleName.asString()
                val tagArray = getTagAsByteArray(prop)
                validateAnnotation(tagArray,className, propertyName)
            }
        }

        private fun processClass(
            classDeclaration: KSClassDeclaration,
            annotatedProperties: Sequence<KSPropertyDeclaration>
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

            annotatedProperties.forEach { prop ->
                val tag = getTagAsString(prop)
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
                    sb.append("        BerTlvEncoder.writeTo(${tag}, it, outputStream)\n")
                    sb.append("    }\n")
                }
            }

            return sb.toString()
        }
    }
}
