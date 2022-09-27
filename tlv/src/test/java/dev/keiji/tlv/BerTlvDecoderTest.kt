package dev.keiji.tlv

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream
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
        assertArrayEquals(expected, actual)
    }

    @Test
    fun readTag2() {
        val data = rand.nextBytes(100)
        data[0] = 0x7F // 01111111
        data[1] = 0x74
        val inputStream = ByteArrayInputStream(data)
        val expected = byteArrayOf(0x7F, 0x74)

        val actual = BerTlvDecoder.readTag(inputStream)
        assertArrayEquals(expected, actual)
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
        assertArrayEquals(expected, actual)
    }

    @Test
    fun readLength1() {
        val data = byteArrayOf(0b0_0000001)
        val inputStream = ByteArrayInputStream(data)
        val expected = BigInteger.ONE

        val actual = BerTlvDecoder.readLength(inputStream)
        assertEquals(expected, actual)
    }

    @Test
    fun readLength2() {
        val data = byteArrayOf(0b1_0000001.toByte(), 127)
        val inputStream = ByteArrayInputStream(data)
        val expected = BigInteger.valueOf(127)

        val actual = BerTlvDecoder.readLength(inputStream)
        assertEquals(expected, actual)
    }

    @Test
    fun readLength3() {
        val data = byteArrayOf(0b1_0000010.toByte(), 0b11111111.toByte(), 0x00000001, 0, 1, 2)
        val inputStream = ByteArrayInputStream(data)
        val expected = BigInteger(+1, byteArrayOf(0b11111111.toByte(), 0x00000001))

        val actual = BerTlvDecoder.readLength(inputStream)
        assertEquals(expected, actual)
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

    @Test
    fun readUnknownLengthTagTest1() {
        val data = byteArrayOf(
            0x01, 0x80.toByte(),
            0x01, 0x02, 0x03,
            0x00, 0x00
        )
        val expected = byteArrayOf(0x01, 0x02, 0x03, 0x00, 0x00)

        var onUndefinedLengthItemDetectedFlag = false

        val inputStream = ByteArrayInputStream(data)
        BerTlvDecoder.readFrom(inputStream, object : BerTlvDecoder.Callback {
            override fun onUnknownLengthItemDetected(tag: ByteArray, inputStream: InputStream) {
                assertArrayEquals(
                    expected,
                    inputStream.readAllBytes()
                )
                onUndefinedLengthItemDetectedFlag = true
            }

            override fun onItemDetected(tag: ByteArray, value: ByteArray) {
                fail()
            }

            override fun onLargeItemDetected(
                tag: ByteArray,
                length: BigInteger,
                inputStream: InputStream
            ) {
                fail()
            }
        })

        assertTrue(onUndefinedLengthItemDetectedFlag)
    }

    @Test
    fun streamFinishedTest1() {
        val data = byteArrayOf(
            0x01, 0x02, 0x01, 0x00,
            0x00, 0x00, // The end of stream
            0x05, 0x01, 0x0F
        )

        BerTlvDecoder.readFrom(
            ByteArrayInputStream(data),
            object : BerTlvDecoder.Callback {
                override fun onItemDetected(tag: ByteArray, value: ByteArray) {
                    if (tag.contentEquals(byteArrayOf(0x01))) {
                        assertArrayEquals(byteArrayOf(0x01, 0x00), value)
                    }
                    if (tag.contentEquals(byteArrayOf(0x00))) {
                        fail()
                    }
                    if (tag.contentEquals(byteArrayOf(0x05))) {
                        assertArrayEquals(byteArrayOf(0x0F), value)
                        fail()
                    }
                }

                override fun onLargeItemDetected(
                    tag: ByteArray,
                    length: BigInteger,
                    inputStream: InputStream
                ) {
                    fail()
                }
            })
    }
}
