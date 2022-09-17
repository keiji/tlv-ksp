
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
import java.io.InputStream
import java.io.InvalidObjectException
import java.io.StreamCorruptedException
import java.math.BigInteger

class BerTlvDecoder {
    companion object {
        private const val MASK_MSB_BITS = 0b100_00000
        private const val MASK_PC_BITS = 0b00_1_00000
        private const val MASK_TAG_BITS = 0b00_0_11111
        private const val MASK_TAG_FULL_BITS: Byte = 0b00_0_11111

        private const val PC_PRIMITIVE = 0b0
        private const val PC_CONSTRUCTED = 0b1

        private const val MAX_LENGTH_FILED_LENGTH = 126

        interface Callback {
            fun onLargeItemDetected(
                tag: ByteArray,
                length: BigInteger,
                inputStream: InputStream
            )

            fun onItemDetected(tag: ByteArray, data: ByteArray)
        }

        fun readFrom(
            inputStream: InputStream,
            callback: Callback,
        ) {
            var tag: ByteArray? = null
            var length: BigInteger? = null

            while (true) {
                if (tag == null) {
                    tag = readTag(inputStream)
                    if (tag == null) {
                        break
                    }
                    continue
                } else if (length == null) {
                    length = readLength(inputStream)
                    continue
                } else {
                    dispatchOnItemDetected(tag, length, inputStream, callback)
                }
                tag = null
                length = null
            }
        }

        private fun isPrimitiveTag(tag: ByteArray): Boolean {
            val firstByte = tag[0].toInt() and 0xFF
            val pcBit = (firstByte and MASK_PC_BITS) ushr 5
            return pcBit == PC_PRIMITIVE
        }

        private fun dispatchOnItemDetected(
            tag: ByteArray,
            length: BigInteger,
            inputStream: InputStream,
            callback: Callback
        ) {
            val isLargeItem = length.bitLength() > (Integer.SIZE - 1)

            if (!isLargeItem) {
                val data = ByteArray(length.toInt())
                inputStream.read(data)
                callback.onItemDetected(tag, data)
            } else {
                callback.onLargeItemDetected(tag, length, inputStream)
            }
        }

        fun readTag(inputStream: InputStream): ByteArray? {
            val tagStream = ByteArrayOutputStream()

            while (true) {
                val b = inputStream.read()
                if (b < 0) {
                    return null
                }

                val tagBits = (b and MASK_TAG_BITS).toByte()

                val tagSize = tagStream.size()

                if (tagSize == 0) {
                    if (tagBits != MASK_TAG_FULL_BITS) {
                        return byteArrayOf(b.toByte())
                    } else {
                        tagStream.write(byteArrayOf(b.toByte()))
                        continue
                    }
                } else {
                    tagStream.write(byteArrayOf(b.toByte()))
                    if (b and MASK_MSB_BITS != 0) {
                        continue
                    } else {
                        return tagStream.toByteArray()
                    }
                }
            }
        }

        internal fun readLength(inputStream: InputStream): BigInteger {
            val b = inputStream.read()
            if (b < 0) {
                throw StreamCorruptedException()
            }

            val longDef = b and MASK_MSB_BITS != 0

            if (!longDef) {
                return BigInteger.valueOf(b.toLong())
            } else {
                val fieldLength = b xor MASK_MSB_BITS
                if (fieldLength > MAX_LENGTH_FILED_LENGTH) {
                    throw InvalidObjectException("Long Definite length must not be grater 126 bytes.")
                }
                val length = ByteArray(fieldLength)
                inputStream.read(length)
                return BigInteger(+1, length)
            }
        }
    }
}
