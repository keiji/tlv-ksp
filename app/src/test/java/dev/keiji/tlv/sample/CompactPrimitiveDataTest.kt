package dev.keiji.tlv.sample

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.StreamCorruptedException

class CompactPrimitiveDataTest {

    @Test
    fun encodeCompactPrimitiveDatumTest1() {
        val data = CompactPrimitiveDatum().also {
            it.data = byteArrayOf(0x00)
        }
        val expected = byteArrayOf(0x11, 0x00)

        val actual = ByteArrayOutputStream().use {
            data.writeTo(it)
            it.toByteArray()
        }

        assertArrayEquals(expected, actual)
    }

    @Test
    fun decodeCompactPrimitiveDatumTest1() {
        val data = byteArrayOf(0x11, 0x00)
        val expected = CompactPrimitiveDatum().also {
            it.data = byteArrayOf(0x00)
        }

        val actual = CompactPrimitiveDatum().also { it.readFrom(data) }

        assertArrayEquals(expected.data, actual.data)
    }

    @Test
    fun decodeCompactPrimitiveDatum_exception1Test() {
        try {
            val data = byteArrayOf(0x11)
            val actual = CompactPrimitiveDatum().also { it.readFrom(data) }
            fail()
        } catch (exception: StreamCorruptedException) {
            println(exception.toString())
        }
    }
}
