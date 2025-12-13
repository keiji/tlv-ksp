package dev.keiji.tlv

import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.lang.IllegalArgumentException
import java.math.BigInteger

class BerTlvEncoderTest {
    @Test
    fun convertToLengthTest1() {
        val expected = byteArrayOf(126)
        val actual = BerTlvEncoder.convertToLength(126)
        Assert.assertArrayEquals(expected, actual)
    }

    @Test
    fun convertToLengthTest2() {
        val expected = byteArrayOf(0b10000001.toByte(), 128.toByte())
        val actual = BerTlvEncoder.convertToLength(128)
        Assert.assertArrayEquals(expected, actual)
    }

    @Test
    fun convertToLengthTest_Exception1() {
        try {
            val actual = BerTlvEncoder.convertToLength(-1)
            Assert.fail()
        } catch (exception: IllegalArgumentException) {
            println(exception)
        }
    }

    @Test
    fun convertToLengthTest3() {
        val expected = byteArrayOf(0x82.toByte(), 0x01, 0x03)
        val actual = BerTlvEncoder.convertToLength(259)
        Assert.assertArrayEquals(expected, actual)
    }

    @Test
    fun writeToTest1() {
        val tag = byteArrayOf(0x01)
        val length = byteArrayOf(0x01)
        val value = byteArrayOf(0xFF.toByte())
        val os = ByteArrayOutputStream()

        BerTlvEncoder.writeTo(tag, length, value, os)

        val result = os.toByteArray()
        val expected = byteArrayOf(0x01, 0x01, 0xFF.toByte())
        Assert.assertArrayEquals(expected, result)
    }

    @Test
    fun writeToTest2() {
        val tag = byteArrayOf(0x01)
        val value = byteArrayOf(0xFF.toByte())
        val os = ByteArrayOutputStream()

        BerTlvEncoder.writeTo(tag, value, os)

        val result = os.toByteArray()
        val expected = byteArrayOf(0x01, 0x01, 0xFF.toByte())
        Assert.assertArrayEquals(expected, result)
    }

    @Test
    fun writeToTest_NullValue() {
        val tag = byteArrayOf(0x01)
        val value: ByteArray? = null
        val os = ByteArrayOutputStream()

        BerTlvEncoder.writeTo(tag, value, os)

        val result = os.toByteArray()
        Assert.assertEquals(0, result.size)
    }

    @Test
    fun convertToLengthTest_LongDefPadding() {
        // longDefLengthFieldSizeAtLeast
        val value = 1
        // Short definition (isLongDef=false) branch is taken if bitLength <= 7.
        // 1 requires 1 bit.
        // However, if longDefLengthFieldSizeAtLeast is specified, it seems we might want to force long definition?
        // But the current implementation checks isLongDef first.
        /*
        val isLongDef = size.bitLength() > 7

        if (!isLongDef) {
            // Short definition
            return byteArrayOf(size.toByte())
        }
        */
        // So if value is 1, it returns short definition regardless of longDefLengthFieldSizeAtLeast.
        // Let's test with a value that forces long definition, e.g., 128 (0x80), or maybe just verify behavior for small values.

        // If the intent of longDefLengthFieldSizeAtLeast is to pad even small values, the implementation ignores it for small values.
        // So let's test with a large value but request MORE padding.

        // Value 128 (0x80). bitLength is 8 (or 7 depending on how you count, but 1000 0000 is 8 bits).
        // BigInteger(128).bitLength() is 8.
        // isLongDef is true.
        // Required bytes: 1.
        // Request at least 2 bytes.

        val valueL = 128
        val expected = byteArrayOf(0x82.toByte(), 0x00, 0x80.toByte())

        val actual = BerTlvEncoder.convertToLength(valueL, longDefLengthFieldSizeAtLeast = 2)
        Assert.assertArrayEquals(expected, actual)
    }

    @Test
    fun convertToLengthTest_FromByteArray() {
        val value = byteArrayOf(0x01)
        val expected = byteArrayOf(0x01)
        val actual = BerTlvEncoder.convertToLength(value)
        Assert.assertArrayEquals(expected, actual)
    }

    @Test
    fun convertToLengthTest_TooLarge() {
        // 126 * 8 = 1008 bits.
        // We need a BigInteger larger than that.
        val large = BigInteger.ONE.shiftLeft(1008 + 1)

        try {
            BerTlvEncoder.convertToLength(large)
            Assert.fail()
        } catch (e: IllegalArgumentException) {
            // Success
        }
    }

    @Test
    fun convertToLengthTest_ByteArrayTrim() {
        // BigInteger.toByteArray() might prepend 0x00 if MSB is 1.
        // 128 (0x80).
        val value = 128
        // 0x81 (length 1), 0x80.
        // BigInteger(128).toByteArray() -> [0x00, 0x80]
        // Code should skip the 0x00.

        val actual = BerTlvEncoder.convertToLength(value)
        val expected = byteArrayOf(0x81.toByte(), 0x80.toByte())
        Assert.assertArrayEquals(expected, actual)
    }
}
