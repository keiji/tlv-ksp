package dev.keiji.tlv

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.InvalidObjectException
import java.io.StreamCorruptedException
import java.math.BigInteger

class BerTlvDecoderTest {

    @Test
    fun readFromTest1() {
        val data = byteArrayOf(0x01, 0x01, 0xFF.toByte())
        val inputStream = ByteArrayInputStream(data)
        val receivedItems = mutableListOf<Pair<ByteArray, ByteArray>>()

        val callback = object : BerTlvDecoder.Callback {
            override fun onItemDetected(tag: ByteArray, value: ByteArray) {
                receivedItems.add(tag to value)
            }
        }

        BerTlvDecoder.readFrom(inputStream, callback)

        assertEquals(1, receivedItems.size)
        assertArrayEquals(byteArrayOf(0x01), receivedItems[0].first)
        assertArrayEquals(byteArrayOf(0xFF.toByte()), receivedItems[0].second)
    }

    @Test
    fun readFromTest_EndOfStream() {
        val data = byteArrayOf(0x00, 0x00)
        val inputStream = ByteArrayInputStream(data)
        val receivedItems = mutableListOf<Pair<ByteArray, ByteArray>>()

        val callback = object : BerTlvDecoder.Callback {
            override fun onItemDetected(tag: ByteArray, value: ByteArray) {
                receivedItems.add(tag to value)
            }
        }

        BerTlvDecoder.readFrom(inputStream, callback)

        assertEquals(0, receivedItems.size)
    }

    @Test
    fun readFromTest_UnknownLength() {
        // Tag 0x01, Length Undefined (0x80)
        val data = byteArrayOf(0x01, 0x80.toByte())
        val inputStream = ByteArrayInputStream(data)

        var unknownCalled = false
        val callback = object : BerTlvDecoder.Callback {
            override fun onItemDetected(tag: ByteArray, value: ByteArray) {
                // Do nothing
            }

            override fun onUnknownLengthItemDetected(tag: ByteArray, inputStream: InputStream) {
                unknownCalled = true
                assertArrayEquals(byteArrayOf(0x01), tag)
            }
        }

        BerTlvDecoder.readFrom(inputStream, callback)
        assertEquals(true, unknownCalled)
    }

    @Test
    fun readFromTest_LargeItem() {
        // Tag 0x01, Length 0x85 (Long definition 5 bytes), Value 0x01 0x00 0x00 0x00 0x00 (4GB)
        // BitLength of 4GB is 33 bits. > 31.
        // We can't actually allocate 4GB in test, but the code checks bitLength of length.
        // length bytes: 0x85, 0x01, 0x00, 0x00, 0x00, 0x00.
        // The stream doesn't need to contain the data if onLargeItemDetected is called and it doesn't read data.

        val header = byteArrayOf(0x01, 0x85.toByte(), 0x01, 0x00, 0x00, 0x00, 0x00)
        val inputStream = ByteArrayInputStream(header)

        var largeCalled = false
        val callback = object : BerTlvDecoder.Callback {
            override fun onItemDetected(tag: ByteArray, value: ByteArray) {
                // Do nothing
            }

            override fun onLargeItemDetected(
                tag: ByteArray,
                length: BigInteger,
                inputStream: InputStream
            ) {
                largeCalled = true
                assertArrayEquals(byteArrayOf(0x01), tag)
                assertEquals(BigInteger("4294967296"), length)
            }
        }

        BerTlvDecoder.readFrom(inputStream, callback)
        assertEquals(true, largeCalled)
    }

    @Test
    fun readTagFieldBytesTest_SingleByte() {
        val data = byteArrayOf(0x01)
        val inputStream = ByteArrayInputStream(data)
        val tag = BerTlvDecoder.readTagFieldBytes(inputStream)
        assertArrayEquals(byteArrayOf(0x01), tag)
    }

    @Test
    fun readTagFieldBytesTest_MultiByte() {
        // 0x1F (0001 1111) indicates multi-byte tag
        // 0x81 (1000 0001) - MSB 1, continue
        // 0x01 (0000 0001) - MSB 0, end
        val data = byteArrayOf(0x1F, 0x81.toByte(), 0x01)
        val inputStream = ByteArrayInputStream(data)
        val tag = BerTlvDecoder.readTagFieldBytes(inputStream)
        assertArrayEquals(byteArrayOf(0x1F, 0x81.toByte(), 0x01), tag)
    }

    @Test
    fun readTagFieldBytesTest_EOF() {
        val inputStream = ByteArrayInputStream(byteArrayOf())
        val tag = BerTlvDecoder.readTagFieldBytes(inputStream)
        assertNull(tag)
    }

    @Test(expected = StreamCorruptedException::class)
    fun readLengthFieldBytes_EOF() {
        val inputStream = ByteArrayInputStream(byteArrayOf())
        BerTlvDecoder.readLengthFieldBytes(inputStream)
    }

    @Test(expected = InvalidObjectException::class)
    fun readLengthFieldBytes_TooLong() {
        // 0xFF -> 127. MAX is 126.
        val inputStream = ByteArrayInputStream(byteArrayOf(0xFF.toByte()))
        BerTlvDecoder.readLengthFieldBytes(inputStream)
    }

    @Test
    fun readValueFieldBytesTest() {
        val data = byteArrayOf(0x01, 0x02, 0x03)
        val inputStream = ByteArrayInputStream(data)
        val value = BerTlvDecoder.readValueFieldBytes(inputStream, BigInteger.valueOf(3))
        assertArrayEquals(data, value)
    }

    @Test(expected = StreamCorruptedException::class)
    fun readValueFieldBytesTest_EOF() {
        val data = byteArrayOf(0x01)
        val inputStream = ByteArrayInputStream(data)
        BerTlvDecoder.readValueFieldBytes(inputStream, BigInteger.valueOf(3))
    }

    @Test
    fun defaultCallbackTest() {
        // Verify default methods of Callback
        val callback = object : BerTlvDecoder.Callback {
            override fun onItemDetected(tag: ByteArray, value: ByteArray) {
                // Do nothing
            }
        }

        // These should not throw exception
        callback.onUnknownLengthItemDetected(byteArrayOf(), ByteArrayInputStream(byteArrayOf()))
        callback.onLargeItemDetected(
            byteArrayOf(),
            BigInteger.ZERO,
            ByteArrayInputStream(byteArrayOf())
        )
    }
}
