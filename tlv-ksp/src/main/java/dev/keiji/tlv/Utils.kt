package dev.keiji.tlv

import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.validate
import java.io.OutputStream

internal fun OutputStream.appendText(str: String): OutputStream {
    this.write(str.toByteArray())
    this.write("\n".toByteArray())
    return this
}

internal fun getTagAsByteArray(prop: KSPropertyDeclaration): ByteArray {
    val fileName = prop.containingFile!!.fileName

    val berTlvItem = prop.annotations
        .filter { it.validate() }
        .firstOrNull { it.shortName.asString() == BerTlvItem::class.simpleName }
    berTlvItem
        ?: throw IllegalArgumentException("BerTlv annotation must be exist.")

    val argument = berTlvItem.arguments
        .filter { it.validate() }
        .firstOrNull { it.name!!.asString() == "tag" }
    argument
        ?: throw IllegalArgumentException("$fileName BerTlv annotation argument `tag` must be exist.")

    val argumentValue = argument.value
    if (argumentValue !is List<*>) {
        throw IllegalArgumentException("$fileName BerTlv annotation argument `tag` value must be instance of List.")
    }
    if (argumentValue.isEmpty()) {
        throw IllegalArgumentException("$fileName BerTlv annotation argument `tag` list must not be empty.")
    }
    if (argumentValue.first() !is Byte) {
        throw IllegalArgumentException("$fileName BerTlv annotation argument `tag` type must be List<Byte>.")
    }

    @Suppress("UNCHECKED_CAST")
    val tagAsByteList = argumentValue as List<Byte>

    return tagAsByteList.toByteArray()
}

internal fun getTagAsString(prop: KSPropertyDeclaration): String {
    val arrayString = getTagAsByteArray(prop).toHex(", ")
    return "byteArrayOf($arrayString)"
}

internal fun ByteArray.toHex(delimiter: String) = this.joinToString(delimiter) { "0x${it.toHex()}" }

internal fun Byte.toHex() = "%02x".format(this).uppercase()
