package dev.keiji.tlv

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import java.lang.StringBuilder

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

            processClass(classDeclaration, annotatedProperties)
        }
    }

    private fun processClass(
        classDeclaration: KSClassDeclaration,
        annotatedProperties: Sequence<KSPropertyDeclaration>
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

        val onItemDetected = generateOnItemDetected(annotatedProperties)

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
        annotatedProperties: Sequence<KSPropertyDeclaration>
    ): String {
        val sb = StringBuilder()

        sb.append("            override fun onItemDetected(tag: ByteArray, data: ByteArray) {\n")
        sb.append("                if (false) {\n")
        sb.append("                    // Do nothing\n")

        annotatedProperties.forEach { prop ->
            val tag = getTagAsString(prop)
            sb.append("                } else if (${tag}.contentEquals(tag)) {\n")

            val decClass = prop.type.resolve().declaration
            if (berTlvClasses.contains(decClass)) {
                val className = decClass.simpleName.asString()
                sb.append("                    this@readFrom.${prop.simpleName.asString()} = ${className}().also { it.readFrom(data) }\n")
            } else {
                sb.append("                    this@readFrom.${prop.simpleName.asString()} = data\n")
            }
        }

        sb.append("                } else {\n")
        sb.append("                    // Do nothing\n")
        sb.append("                }\n")
        sb.append("            }\n")
        return sb.toString()
    }
}
