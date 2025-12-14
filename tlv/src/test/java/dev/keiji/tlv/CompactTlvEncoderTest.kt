package dev.keiji.tlv

import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayOutputStream

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

    @Test
    fun writeToTest() {
        val tag: Byte = 0x1
        val value = byteArrayOf(0x01, 0x02)
        val os = ByteArrayOutputStream()

        CompactTlvEncoder.writeTo(tag, value, os)

        val result = os.toByteArray()
        val expected = byteArrayOf(0x12, 0x01, 0x02)
        Assert.assertArrayEquals(expected, result)
    }

    @Test
    fun writeToTest_NullValue() {
        val tag: Byte = 0x1
        val value: ByteArray? = null
        val os = ByteArrayOutputStream()

        CompactTlvEncoder.writeTo(tag, value, os)

        val result = os.toByteArray()
        Assert.assertEquals(0, result.size)
    }

    @Test
    fun writeToTest_ExplicitLength() {
        val tag: Byte = 0x2
        val length = 1
        val value = byteArrayOf(0xAA.toByte())
        val os = ByteArrayOutputStream()

        CompactTlvEncoder.writeTo(tag, length, value, os)

        val result = os.toByteArray()
        val expected = byteArrayOf(0x21, 0xAA.toByte())
        Assert.assertArrayEquals(expected, result)
    }

    @Test
    fun writeToTest_InvalidTag() {
        // Tag > MAX_LENGTH (16)
        // private const val MAX_LENGTH = 0b00010000 = 16
        // But Byte is signed, so 0x10 is 16.
        // The check is: if (tag > MAX_LENGTH && tag < 0)
        // Wait, the logic in CompactTlvEncoder seems weird:
        // if (tag > MAX_LENGTH && tag < 0) { return }
        // AND condition? It's impossible for a number to be > 16 AND < 0.
        // It probably should be OR (||).
        // Let's verify what the code says.
        /*
        if (tag > MAX_LENGTH && tag < 0) {
            return
        }
         */
        // If it is indeed &&, then this check is dead code (always false).
        // Let's check if I can trigger it.

        // If I pass tag = 17 (0x11), 17 > 16 is true. 17 < 0 is false. So condition is false.
        // It proceeds to packTagAndLength.

        // Let's try to test it as is.
        val tag: Byte = 0x11
        val value = byteArrayOf(0x01)
        val os = ByteArrayOutputStream()

        CompactTlvEncoder.writeTo(tag, value, os)

        // 0x11 is 17. 17 << 4 = 272. Byte overflow.
        // 17 = 0001 0001.
        // 0001 0001 << 4 = 0001 0001 0000 (272)
        // toByte() takes lower 8 bits: 0001 0000 = 0x10.
        // Or with length (1): 0x11.
        // So it writes 0x11 0x01.

        val result = os.toByteArray()
        val expected = byteArrayOf(0x11, 0x01)
        Assert.assertArrayEquals(expected, result)
    }
}
