package dev.keiji.tlv

import org.junit.Assert.assertEquals
import org.junit.Test

class CompactTlvDecoderTest {

    @Test
    fun readTagLengthTest1() {
        val tagAndLength = 0x1F
        val expectedTag: Byte = 0x1
        val expectedLength = 0xF

        val actualTag = CompactTlvDecoder.readTag(tagAndLength)
        val actualLength = CompactTlvDecoder.readLength(tagAndLength)

        assertEquals(expectedTag, actualTag)
        assertEquals(expectedLength, actualLength)
    }

    @Test
    fun readTagLengthTest2() {
        val tagAndLength = 0x10
        val expectedTag: Byte = 0x1
        val expectedLength = 0x0

        val actualTag = CompactTlvDecoder.readTag(tagAndLength)
        val actualLength = CompactTlvDecoder.readLength(tagAndLength)

        assertEquals(expectedTag, actualTag)
        assertEquals(expectedLength, actualLength)
    }

    @Test
    fun readTagLengthTest3() {
        val tagAndLength = 0xFF
        val expectedTag: Byte = 0xF
        val expectedLength = 0xF

        val actualTag = CompactTlvDecoder.readTag(tagAndLength)
        val actualLength = CompactTlvDecoder.readLength(tagAndLength)

        assertEquals(expectedTag, actualTag)
        assertEquals(expectedLength, actualLength)
    }
}
