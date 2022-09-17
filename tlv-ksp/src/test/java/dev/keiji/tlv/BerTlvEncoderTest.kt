package dev.keiji.tlv

import org.junit.Assert
import org.junit.Test
import java.lang.IllegalArgumentException

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
}
