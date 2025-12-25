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
    fun writeToTest_InvalidTag_TooLarge() {
        val tag: Byte = 0x10 // 16
        val value = byteArrayOf(0x01)
        val os = ByteArrayOutputStream()

        CompactTlvEncoder.writeTo(tag, value, os)

        val result = os.toByteArray()
        Assert.assertEquals(0, result.size)
    }

    @Test
    fun writeToTest_InvalidTag_Negative() {
        val tag: Byte = -1
        val value = byteArrayOf(0x01)
        val os = ByteArrayOutputStream()

        CompactTlvEncoder.writeTo(tag, value, os)

        val result = os.toByteArray()
        Assert.assertEquals(0, result.size)
    }

    @Test
    fun writeToTest_InvalidLength_TooLarge() {
        val tag: Byte = 0x1
        val length = 16
        val value = byteArrayOf(0x01) // value size ignored when explicit length passed?
        // writeTo(tag, length, value, os) uses passed length.
        val os = ByteArrayOutputStream()

        CompactTlvEncoder.writeTo(tag, length, value, os)

        val result = os.toByteArray()
        Assert.assertEquals(0, result.size)
    }

    @Test
    fun writeToTest_InvalidLength_Negative() {
        val tag: Byte = 0x1
        val length = -1
        val value = byteArrayOf(0x01)
        val os = ByteArrayOutputStream()

        CompactTlvEncoder.writeTo(tag, length, value, os)

        val result = os.toByteArray()
        Assert.assertEquals(0, result.size)
    }
}
