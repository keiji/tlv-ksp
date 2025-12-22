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

import java.io.OutputStream

/**
 * Compact-TLV Encoder.
 */
@Suppress("MagicNumber")
object CompactTlvEncoder {
    private const val MAX_LENGTH = 0b00010000

    /**
     * Write a TLV-item to OutputStream.
     *
     * @param tag
     * @param length
     * @param value
     * @param os
     */
    fun writeTo(
        tag: Byte,
        length: Int,
        value: ByteArray,
        os: OutputStream
    ) {
        if (tag >= MAX_LENGTH || tag < 0) {
            return
        }
        if (length >= MAX_LENGTH || length < 0) {
            return
        }
        val tagAndLength = packTagAndLength(tag, length)
        os.write(byteArrayOf(tagAndLength))
        os.write(value)
    }

    fun packTagAndLength(tag: Byte, length: Int): Byte =
        ((tag.toInt() shl 4) or length).toByte()

    /**
     * Write a TLV-item to OutputStream.
     *
     * @param tag
     * @param value
     * @param os
     */
    fun writeTo(
        tag: Byte,
        value: ByteArray?,
        os: OutputStream
    ) {
        value ?: return

        writeTo(tag, value.size, value, os)
    }
}
