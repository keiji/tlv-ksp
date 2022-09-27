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

import java.nio.charset.Charset

abstract class AbsTypeConverter<T : Any> {
    abstract fun convertFromByteArray(byteArray: ByteArray): T?
    abstract fun convertToByteArray(data: T): ByteArray
}

class NopConverter : AbsTypeConverter<ByteArray>() {
    override fun convertFromByteArray(byteArray: ByteArray): ByteArray = byteArray
    override fun convertToByteArray(data: ByteArray): ByteArray = data
}

class ByteTypeConverter : AbsTypeConverter<Byte>() {
    override fun convertFromByteArray(byteArray: ByteArray): Byte = byteArray[0]
    override fun convertToByteArray(data: Byte): ByteArray = byteArrayOf(data)
}

class BooleanTypeConverter : AbsTypeConverter<Boolean>() {
    override fun convertFromByteArray(byteArray: ByteArray): Boolean {
        return byteArray[0] != 0x00.toByte()
    }

    override fun convertToByteArray(data: Boolean): ByteArray {
        return byteArrayOf(if (data) 0xFF.toByte() else 0x00)
    }
}

private val CHARSET_UTF8 = Charset.forName("UTF-8")

class StringTypeConverter : AbsTypeConverter<String>() {
    override fun convertFromByteArray(byteArray: ByteArray): String {
        return String(byteArray, charset = CHARSET_UTF8)
    }

    override fun convertToByteArray(data: String): ByteArray {
        return data.toByteArray(charset = CHARSET_UTF8)
    }
}
