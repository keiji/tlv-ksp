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

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.validate
import java.io.OutputStream
import java.lang.Integer.max
import java.util.stream.IntStream.range

internal fun OutputStream.appendText(str: String): OutputStream {
    this.write(str.toByteArray())
    this.write("\n".toByteArray())
    return this
}

internal val annotatedPropertyOrderComparator =
    Comparator<KSPropertyDeclaration> { obj1, obj2 ->
        if (obj1 === obj2) {
            return@Comparator 0
        }

        val obj1Order = getOrder(obj1)
        val obj2Order = getOrder(obj2)

        return@Comparator obj1Order.compareTo(obj2Order)
    }

internal fun getOrder(prop: KSPropertyDeclaration): Int {
    val fileName = prop.qualifiedName!!.asString()

    val berTlvItem = prop.annotations
        .filter { it.validate() }
        .firstOrNull { it.shortName.asString() == BerTlvItem::class.simpleName }
    berTlvItem
        ?: throw IllegalArgumentException("BerTlv annotation must be exist.")

    val argument = berTlvItem.arguments
        .filter { it.validate() }
        .firstOrNull { it.name!!.asString() == "order" }
    argument
        ?: throw IllegalArgumentException("$fileName BerTlv annotation argument `order` must be exist.")

    val argumentValue = argument.value
    if (argumentValue !is Int) {
        throw IllegalArgumentException("$fileName BerTlv annotation argument `order` value must be instance of Int.")
    }

    return argumentValue
}

internal val annotatedPropertyTagComparator =
    Comparator<KSPropertyDeclaration> { obj1, obj2 ->
        if (obj1 === obj2) {
            return@Comparator 0
        }

        val obj1Tag = getTagAsByteArray(obj1)
        val obj2Tag = getTagAsByteArray(obj2)

        return@Comparator compare(obj1Tag, obj2Tag)
    }

internal fun compare(byteArray1: ByteArray, byteArray2: ByteArray): Int {
    if (byteArray1 === byteArray2) {
        return 0
    }

    val size = max(byteArray1.size, byteArray2.size)

    var result = 0

    range(0, size).forEach { index ->
        if (byteArray1.size > index && byteArray2.size > index) {
            val value1: Int = byteArray1[index].toInt() and 0xFF
            val value2: Int = byteArray2[index].toInt() and 0xFF
            result = value1.compareTo(value2)
            if (result != 0) {
                return@forEach
            }
        } else if (byteArray1.size <= index) {
            result = -1
            return@forEach
        } else { // if (byteArray2.size <= index)
            result = +1
            return@forEach
        }
    }

    return result
}

internal fun getTagAsByteArray(prop: KSPropertyDeclaration): ByteArray {
    val fileName = prop.qualifiedName!!.asString()

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

internal fun getTagAsString(
    prop: KSPropertyDeclaration,
    logger: KSPLogger,
): String {
    val arrayString = getTagAsByteArray(prop)
        .joinToString(", ") { "0x${it.toHex()}.toByte()" }
    return "byteArrayOf($arrayString)"
}

internal fun ByteArray.toHex(delimiter: String) = this.joinToString(delimiter) { "0x${it.toHex()}" }

internal fun Byte.toHex() = "%02x".format(this).uppercase()
