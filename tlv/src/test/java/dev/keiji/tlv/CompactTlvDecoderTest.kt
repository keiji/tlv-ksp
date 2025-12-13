package dev.keiji.tlv

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.StreamCorruptedException

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

    @Test
    fun readFromTest() {
        val data = byteArrayOf(0x11, 0x01, 0x22, 0x01, 0x02)
        val inputStream = ByteArrayInputStream(data)
        val receivedItems = mutableListOf<Pair<Byte, ByteArray>>()
        val callback = object : CompactTlvDecoder.Callback {
            override fun onItemDetected(tag: Byte, value: ByteArray) {
                receivedItems.add(tag to value)
            }
        }

        CompactTlvDecoder.readFrom(inputStream, callback)

        assertEquals(2, receivedItems.size)
        assertEquals(0x01.toByte(), receivedItems[0].first)
        assertArrayEquals(byteArrayOf(0x01), receivedItems[0].second)
        assertEquals(0x02.toByte(), receivedItems[1].first)
        assertArrayEquals(byteArrayOf(0x01, 0x02), receivedItems[1].second)
    }

    @Test
    fun readFromTest_Empty() {
        val data = byteArrayOf()
        val inputStream = ByteArrayInputStream(data)
        val receivedItems = mutableListOf<Pair<Byte, ByteArray>>()
        val callback = object : CompactTlvDecoder.Callback {
            override fun onItemDetected(tag: Byte, value: ByteArray) {
                receivedItems.add(tag to value)
            }
        }

        CompactTlvDecoder.readFrom(inputStream, callback)

        assertEquals(0, receivedItems.size)
    }

    @Test
    fun readFromTest_ZeroLength() {
        val data = byteArrayOf(0x10) // Tag 1, Length 0
        val inputStream = ByteArrayInputStream(data)
        val receivedItems = mutableListOf<Pair<Byte, ByteArray>>()
        val callback = object : CompactTlvDecoder.Callback {
            override fun onItemDetected(tag: Byte, value: ByteArray) {
                receivedItems.add(tag to value)
            }
        }

        CompactTlvDecoder.readFrom(inputStream, callback)

        // Callback is not called for length 0 in the implementation
        // if (length == 0) { continue }
        assertEquals(0, receivedItems.size)
    }

    @Test(expected = StreamCorruptedException::class)
    fun readValue_StreamCorruptedException() {
        // Tag 1, Length 5, but stream ends
        val data = byteArrayOf(0x15, 0x01, 0x02)
        val inputStream = ByteArrayInputStream(data)
        val callback = object : CompactTlvDecoder.Callback {
            override fun onItemDetected(tag: Byte, value: ByteArray) {
            }
        }
        CompactTlvDecoder.readFrom(inputStream, callback)
    }

    @Test
    fun readValue_RetryLoop() {
        // Simulate a slow stream or similar where read returns less than requested but not -1 initially?
        // Actually ByteArrayInputStream returns all available.
        // To test the loop: while (true) { ... if (readLength < (dataLength - offset)) ... }
        // We can create a custom InputStream to simulate partial reads.

        val data = byteArrayOf(0x12, 0xAA.toByte(), 0xBB.toByte())
        val inputStream = object : InputStream() {
            var index = 0
            val src = data

            override fun read(): Int {
                if (index >= src.size) return -1
                return src[index++].toInt() and 0xFF
            }

            override fun read(b: ByteArray, off: Int, len: Int): Int {
                if (index >= src.size) return -1
                // Return 1 byte at a time
                val byte = read()
                if (byte == -1) return -1
                b[off] = byte.toByte()
                return 1
            }
        }

        val receivedItems = mutableListOf<Pair<Byte, ByteArray>>()
        val callback = object : CompactTlvDecoder.Callback {
            override fun onItemDetected(tag: Byte, value: ByteArray) {
                receivedItems.add(tag to value)
            }
        }

        CompactTlvDecoder.readFrom(inputStream, callback)

        assertEquals(1, receivedItems.size)
        assertEquals(0x01.toByte(), receivedItems[0].first)
        assertArrayEquals(byteArrayOf(0xAA.toByte(), 0xBB.toByte()), receivedItems[0].second)
    }

    @Test
    fun constructorTest() {
        val decoder = CompactTlvDecoder()
        // just to cover constructor
        org.junit.Assert.assertNotNull(decoder)
    }
}
