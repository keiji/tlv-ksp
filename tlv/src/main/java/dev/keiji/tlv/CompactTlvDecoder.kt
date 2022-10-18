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

import java.io.InputStream
import java.io.StreamCorruptedException

/**
 * Compact-TLV Decoder.
 */
class CompactTlvDecoder {

    companion object {
        private const val MASK_TAG_BITS: Int = 0b11110000
        private const val MASK_LENGTH_BITS: Int = 0b00001111

        /**
         * Read TLV-item(s) from the InputStream.
         *
         * @param inputStream
         * @param callback Callback to receive event about detect TLV-item
         */
        fun readFrom(
            inputStream: InputStream,
            callback: Callback,
        ) {
            while (true) {
                val tagAndLength = inputStream.read()
                if (tagAndLength < 0) {
                    break
                }

                val tag = readTag(tagAndLength)
                val length = readLength(tagAndLength)

                if (length == 0) {
                    continue
                }

                val value = readValue(inputStream, length)
                callback.onItemDetected(tag, value)
            }
        }

        fun readValue(
            inputStream: InputStream,
            dataLength: Int,
        ): ByteArray {
            val data = ByteArray(dataLength)
            var offset = 0

            while (true) {
                val readLength = inputStream.read(data, offset, (dataLength - offset))
                if (readLength < 0) {
                    throw StreamCorruptedException()
                } else if (readLength < (dataLength - offset)) {
                    offset += readLength
                } else {
                    return data
                }
            }
        }

        fun readTag(tagAndLength: Int): Byte = ((tagAndLength and MASK_TAG_BITS) ushr 4).toByte()

        fun readLength(tagAndLength: Int): Int = tagAndLength and MASK_LENGTH_BITS
    }

    /**
     * An interface to receive event about detect TLV-item on InputStream.
     * The Callback is set with CompactTlvDecoder.readFrom method.
     */
    interface Callback {

        /**
         * Called when a TLV-item has been detected.
         *
         * @param tag
         * @param value
         */
        fun onItemDetected(tag: Byte, value: ByteArray)
    }
}
