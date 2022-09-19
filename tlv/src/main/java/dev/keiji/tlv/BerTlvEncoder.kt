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

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.math.BigInteger

class BerTlvEncoder {
    companion object {
        fun writeTo(
            tag: ByteArray,
            length: ByteArray,
            content: ByteArray,
            os: OutputStream
        ) {
            os.write(tag)
            os.write(length)
            os.write(content)
        }

        fun writeTo(
            tag: ByteArray,
            content: ByteArray?,
            os: OutputStream
        ) {
            content ?: return

            os.write(tag)
            os.write(convertToLength(content))
            os.write(content)
        }

        internal fun convertToLength(array: ByteArray): ByteArray {
            return convertToLength(array.size)
        }

        internal fun convertToLength(value: Int): ByteArray {
            return convertToLength(BigInteger.valueOf(value.toLong()))
        }

        internal fun convertToLength(size: BigInteger): ByteArray {
            if (size.compareTo(BigInteger.ZERO) == -1) {
                throw IllegalArgumentException("size must not be less than 0.")
            }

            if (size.bitLength() > 126 * 8) {
                throw IllegalArgumentException("size length must not be greater or equal than 126 bytes.")
            }

            val isLongDef = size.bitLength() > 7

            if (!isLongDef) {
                // Short definition
                return byteArrayOf(size.toByte())
            }

            // Long definition
            return ByteArrayOutputStream().let { baos ->
                var bitLength = size.bitLength()
                bitLength += if (bitLength % 8 == 0) 0 else 1

                val sizeBits = 0b10000000 or (bitLength / 8)
                val sizeBytes = size.toByteArray()
                val offset = if (sizeBytes[0] == 0x00.toByte()) 1 else 0

                baos.write(sizeBits)
                baos.write(sizeBytes, offset, (sizeBytes.size - offset))
                baos.toByteArray()
            }
        }
    }
}
