package dev.keiji.tlv.sample

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.StreamCorruptedException

class CompactPrimitiveDataTest {

    @Test
    fun encodeCompactPrimitiveDatumTest1() {
        val data = CompactPrimitiveDatum().also {
            it.data1 = byteArrayOf(0x00)
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
            it.data1 = byteArrayOf(0x00)
        }

        val actual = CompactPrimitiveDatum().also { it.readFrom(data) }

        assertArrayEquals(expected.data1, actual.data1)
    }

    @Test
    fun encodeCompactPrimitiveDatumTest2() {
        val data = CompactPrimitiveDatum().also {
            it.data1 = byteArrayOf(0x00)
            it.data2 = byteArrayOf(0x10)
        }
        val expected = byteArrayOf(0x21, 0x10, 0x11, 0x00)

        val actual = ByteArrayOutputStream().use {
            data.writeTo(it)
            it.toByteArray()
        }

        assertArrayEquals(expected, actual)
    }

    @Test
    fun decodeCompactPrimitiveDatumTest2() {
        val data = byteArrayOf(0x11, 0x00, 0x21, 0x10)
        val expected = CompactPrimitiveDatum().also {
            it.data1 = byteArrayOf(0x00)
            it.data2 = byteArrayOf(0x10)
        }

        val actual = CompactPrimitiveDatum().also { it.readFrom(data) }

        assertArrayEquals(expected.data1, actual.data1)
        assertArrayEquals(expected.data2, actual.data2)
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
