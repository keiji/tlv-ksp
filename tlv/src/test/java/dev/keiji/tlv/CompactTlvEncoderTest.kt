package dev.keiji.tlv

import org.junit.Assert
import org.junit.Test
import java.lang.IllegalArgumentException

class CompactTlvEncoderTest {

    @Test
    fun packTagLengthTest1() {
        val tag: Byte = 0x1
        val length = 0xF
        val expected: Byte = 0x1F

        val actual = CompactTlvEncoder.packTagAndLength(tag, length)

        Assert.assertEquals(expected, actual)
    }

    @Test
    fun packTagLengthTest2() {
        val tag: Byte = 0x1
        val length = 0x0
        val expected: Byte = 0x10

        val actual = CompactTlvEncoder.packTagAndLength(tag, length)

        Assert.assertEquals(expected, actual)
    }

    @Test
    fun packTagLengthTest3() {
        val tag: Byte = 0xF
        val length = 0xF
        val expected: Byte = 0xFF.toByte()

        val actual = CompactTlvEncoder.packTagAndLength(tag, length)

        Assert.assertEquals(expected, actual)
    }
}
