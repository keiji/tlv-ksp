package dev.keiji.tlv

import org.junit.Assert
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InvalidObjectException
import java.math.BigInteger
import kotlin.random.Random

class BerTlvDecoderTest {

    private val rand = Random(System.currentTimeMillis())

    @Test
    fun readTag1() {
        val data = rand.nextBytes(100)
        data[0] = 0x4F
        val inputStream = ByteArrayInputStream(data)
        val expected = byteArrayOf(0x4F)

        val actual = BerTlvDecoder.readTag(inputStream)
        Assert.assertArrayEquals(expected, actual)
    }

    @Test
    fun readTag2() {
        val data = rand.nextBytes(100)
        data[0] = 0x7F // 01111111
        data[1] = 0x74
        val inputStream = ByteArrayInputStream(data)
        val expected = byteArrayOf(0x7F, 0x74)

        val actual = BerTlvDecoder.readTag(inputStream)
        Assert.assertArrayEquals(expected, actual)
    }

    @Test
    fun readTag3() {
        val data = rand.nextBytes(100)
        data[0] = 0x7F // 01111111
        data[1] = 0x84.toByte()
        data[2] = 0x74.toByte()
        val inputStream = ByteArrayInputStream(data)
        val expected = byteArrayOf(0x7F, 0x84.toByte(), 0x74)

        val actual = BerTlvDecoder.readTag(inputStream)
        Assert.assertArrayEquals(expected, actual)
    }

    @Test
    fun readLength1() {
        val data = byteArrayOf(0b0_0000001)
        val inputStream = ByteArrayInputStream(data)
        val expected = BigInteger.ONE

        val actual = BerTlvDecoder.readLength(inputStream)
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun readLength2() {
        val data = byteArrayOf(0b1_0000001.toByte(), 127)
        val inputStream = ByteArrayInputStream(data)
        val expected = BigInteger.valueOf(127)

        val actual = BerTlvDecoder.readLength(inputStream)
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun readLength3() {
        val data = byteArrayOf(0b1_0000010.toByte(), 0b11111111.toByte(), 0x00000001, 0, 1, 2)
        val inputStream = ByteArrayInputStream(data)
        val expected = BigInteger(+1, byteArrayOf(0b11111111.toByte(), 0x00000001))

        val actual = BerTlvDecoder.readLength(inputStream)
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun readLength4_exception() {
        val data = rand.nextBytes(128)
        data[0] = (0b1_0000010 or 127).toByte()
        val inputStream = ByteArrayInputStream(data)
        try {
            val actual = BerTlvDecoder.readLength(inputStream)
            fail()
        } catch (exception: InvalidObjectException) {
            System.out.println(exception)
        }
    }
}
