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
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.validate
import java.io.OutputStream
import java.lang.Integer.max
import java.util.stream.IntStream.range
import kotlin.reflect.KClass

internal fun OutputStream.appendText(str: String): OutputStream {
    this.write(str.toByteArray())
    this.write("\n".toByteArray())
    return this
}

internal fun getOrder(
    prop: KSPropertyDeclaration,
    annotationClass: KClass<*>,
): Int {
    val fileName = prop.qualifiedName!!.asString()

    val item = prop.annotations
        .filter { it.validate() }
        .firstOrNull { it.shortName.asString() == annotationClass.simpleName }
    requireNotNull(item) { "${annotationClass.simpleName} annotation must be exist." }

    val argument = item.arguments
        .filter { it.validate() }
        .firstOrNull { it.name!!.asString() == "order" }
    requireNotNull(argument) {
        "$fileName ${annotationClass.simpleName} annotation argument `order` must be exist."
    }

    val argumentValue = argument.value
    require(argumentValue is Int) {
        "$fileName ${annotationClass.simpleName} annotation argument `order` value must be instance of Int."
    }

    return argumentValue
}

internal val annotatedPropertyTagComparator =
    Comparator<KSPropertyDeclaration> { obj1, obj2 ->
        if (obj1 === obj2) {
            return@Comparator 0
        }

        val obj1Tag = getTagAsByteArray(obj1, BerTlvItem::class)
        val obj2Tag = getTagAsByteArray(obj2, BerTlvItem::class)

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

internal fun lead(
    className: String,
    propertyName: String
): String {
    return if (className.isNotEmpty() && propertyName.isNotEmpty()) {
        "Class $className property $propertyName"
    } else {
        ""
    }
}

internal fun getTagAsByteArray(
    prop: KSPropertyDeclaration,
    annotationClass: KClass<*>,
): ByteArray {
    val fileName = prop.qualifiedName!!.asString()

    val item = prop.annotations
        .filter { it.validate() }
        .firstOrNull { it.shortName.asString() == annotationClass.simpleName }
    requireNotNull(item) { "${annotationClass.simpleName} annotation must be exist." }

    val argument = item.arguments
        .filter { it.validate() }
        .firstOrNull { it.name!!.asString() == "tag" }
    requireNotNull(argument) {
        "$fileName ${annotationClass.simpleName} annotation argument `tag` must be exist."
    }

    val argumentValue = argument.value
    require(argumentValue is List<*>) {
        "$fileName ${annotationClass.simpleName} annotation argument `tag` value must be instance of List."
    }
    require(argumentValue.isNotEmpty()) {
        "$fileName ${annotationClass.simpleName} annotation argument `tag` list must not be empty."
    }
    require(argumentValue.first() is Byte) {
        "$fileName ${annotationClass.simpleName} annotation argument `tag` type must be List<Byte>."
    }

    @Suppress("UNCHECKED_CAST")
    val tagAsByteList = argumentValue as List<Byte>

    return tagAsByteList.toByteArray()
}

internal fun getTagAsByte(
    prop: KSPropertyDeclaration,
    annotationClass: KClass<*>,
): Byte {
    val fileName = prop.qualifiedName!!.asString()

    val item = prop.annotations
        .filter { it.validate() }
        .firstOrNull { it.shortName.asString() == annotationClass.simpleName }
    requireNotNull(item) { "${annotationClass.simpleName} annotation must be exist." }

    val argument = item.arguments
        .filter { it.validate() }
        .firstOrNull { it.name!!.asString() == "tag" }
    requireNotNull(argument) {
        "$fileName ${annotationClass.simpleName} annotation argument `tag` must be exist."
    }

    val argumentValue = argument.value
    require(argumentValue is Byte) {
        "$fileName ${annotationClass.simpleName} annotation argument `tag` value" +
                " must be instance of Int. ${argumentValue?.javaClass?.simpleName}"
    }

    return argumentValue
}

internal fun getTagArrayAsString(
    prop: KSPropertyDeclaration,
    annotationClass: KClass<*>,
    logger: KSPLogger,
): String {
    return getTagAsByteArray(prop, annotationClass)
        .joinToString(", ") { "0x${it.toHex()}.toByte()" }
}

internal fun getTagAsString(
    prop: KSPropertyDeclaration,
    annotationClass: KClass<*>,
    logger: KSPLogger,
): String {
    return "0x${getTagAsByte(prop, annotationClass).toHex()}.toByte()"
}

internal fun getLongDefLengthFieldSizeAtLeast(
    prop: KSPropertyDeclaration,
    annotationClass: KClass<*>,
    logger: KSPLogger,
): Int {
    val fileName = prop.qualifiedName!!.asString()

    val item = prop.annotations
        .filter { it.validate() }
        .firstOrNull { it.shortName.asString() == annotationClass.simpleName }
    requireNotNull(item) { "${annotationClass.simpleName} annotation must be exist." }

    val argument = item.arguments
        .filter { it.validate() }
        .firstOrNull { it.name!!.asString() == "longDefLengthFieldSizeAtLeast" }
    requireNotNull(argument) {
        "$fileName ${annotationClass.simpleName} annotation argument" +
                " `longDefLengthFieldSizeAtLeast` must be exist."
    }

    val argumentValue = argument.value
    require(argumentValue is Int) {
        "$fileName ${annotationClass.simpleName} annotation argument" +
                " `longDefLengthFieldSizeAtLeast` value must be instance of Int."
    }

    return argumentValue
}

internal fun getQualifiedName(
    prop: KSPropertyDeclaration,
    annotationClass: KClass<*>,
    logger: KSPLogger,
): String {
    val fileName = prop.qualifiedName!!.asString()

    val item = prop.annotations
        .filter { it.validate() }
        .firstOrNull { it.shortName.asString() == annotationClass.simpleName }
    requireNotNull(item) { "${annotationClass.simpleName} annotation must be exist." }

    val argument = item.arguments
        .filter { it.validate() }
        .firstOrNull { it.name!!.asString() == "typeConverter" }
    requireNotNull(argument) {
        "$fileName ${annotationClass.simpleName} annotation argument `typeConverter` must be exist."
    }

    val argumentValue = argument.value as KSType

    return argumentValue.declaration.qualifiedName!!.asString()
}

internal fun generateVariableName(qualifiedName: String): String = qualifiedName.replace(".", "_")
