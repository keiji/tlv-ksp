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
            os.write(convertToLength(content.size))
            os.write(content)
        }

        fun convertToLength(array: ByteArray): ByteArray {
            return convertToLength(BigInteger.valueOf(array.size.toLong()))
        }

        internal fun convertToLength(size: Int): ByteArray {
            return convertToLength(BigInteger.valueOf(size.toLong()))
        }

        private fun convertToLength(size: BigInteger): ByteArray {
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
