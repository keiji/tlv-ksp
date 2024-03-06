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
import java.lang.Math.max
import java.math.BigInteger

/**
 * BER-TLV Encoder.
 */
@Suppress("MagicNumber")
object BerTlvEncoder {
    /**
     * Write a TLV-item to OutputStream.
     *
     * @param tag
     * @param length
     * @param value
     * @param os
     */
    fun writeTo(
        tag: ByteArray,
        length: ByteArray,
        value: ByteArray,
        os: OutputStream
    ) {
        os.write(tag)
        os.write(length)
        os.write(value)
    }

    /**
     * Write a TLV-item to OutputStream.
     *
     * @param tag
     * @param value
     * @param os
     */
    fun writeTo(
        tag: ByteArray,
        value: ByteArray?,
        os: OutputStream,
        longDefLengthFieldSizeAtLeast: Int = 0,
    ) {
        value ?: return

        os.write(tag)
        os.write(convertToLength(value, longDefLengthFieldSizeAtLeast))
        os.write(value)
    }

    fun convertToLength(
        array: ByteArray,
        longDefLengthFieldSizeAtLeast: Int = 0,
    ): ByteArray {
        return convertToLength(
            array.size,
            longDefLengthFieldSizeAtLeast,
        )
    }

    fun convertToLength(
        value: Int,
        longDefLengthFieldSizeAtLeast: Int = 0,
    ): ByteArray {
        return convertToLength(
            BigInteger.valueOf(value.toLong()),
            longDefLengthFieldSizeAtLeast,
        )
    }

    fun convertToLength(
        size: BigInteger,
        longDefLengthFieldSizeAtLeast: Int = 0,
    ): ByteArray {
        require(size.compareTo(BigInteger.ZERO) != -1) { "size must not be less than 0." }
        require(size.bitLength() <= 126 * 8) {
            "size length must not be greater or equal than 126 bytes."
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

            val longLengthFieldBytesSize = (bitLength / Byte.SIZE_BITS) +
                    if ((bitLength % Byte.SIZE_BITS) != 0) 1 else 0

            val lengthSize = max(longLengthFieldBytesSize, longDefLengthFieldSizeAtLeast)
            val sizeBits = 0b10000000 or lengthSize

            val sizeBytes = size.toByteArray()

            // BigInteger.toByteArray() will append 0x00 byte if the byte of byteArray[0] MSB is 1.
            // Here is trim needless 0x00.
            val offset = if (sizeBytes[0] == 0x00.toByte()) 1 else 0

            val lengthBytes = ByteArray(lengthSize)
            sizeBytes.copyInto(
                lengthBytes,
                destinationOffset = lengthSize - longLengthFieldBytesSize,
                startIndex = offset,
                endIndex = sizeBytes.size,
            )

            baos.write(sizeBits)
            baos.write(lengthBytes)
            baos.toByteArray()
        }
    }
}
