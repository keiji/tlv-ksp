package dev.keiji.tlv.sample

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.StreamCorruptedException
import kotlin.random.Random

class PrimitiveDataTest {
    private val rand = Random(System.currentTimeMillis())

    @Test
    fun encodePrimitiveDatumTest() {
        val data = PrimitiveDatum().also {
            it.data = byteArrayOf(0x00)
        }
        val expected = byteArrayOf(0x01, 0x01, 0x00)

        val actual = ByteArrayOutputStream().use {
            data.writeTo(it)
            it.toByteArray()
        }

        assertArrayEquals(expected, actual)
    }

    @Test
    fun decodePrimitiveDatumTest() {
        val data = byteArrayOf(0x01, 0x01, 0x00)
        val expected = PrimitiveDatum().also {
            it.data = byteArrayOf(0x00)
        }

        val actual = PrimitiveDatum().also { it.readFrom(data) }

        assertArrayEquals(expected.data, actual.data)
    }

    @Test
    fun decodePrimitiveDatumCorruptedTest() {
        val data = byteArrayOf(0x01, 0x01)

        try {
            val actual = PrimitiveDatum().also { it.readFrom(data) }
            fail()
        } catch (exception: StreamCorruptedException) {
            println(exception)
        }
    }

    @Test
    fun encodePrimitiveDatumShortDefiniteLengthTest() {
        val dataArray = rand.nextBytes(127) // Short-Definite Length

        val data = PrimitiveDatum().also {
            it.data = dataArray
        }
        val expected = ByteArrayOutputStream().use {
            it.write(0x01)
            it.write(127)
            it.write(dataArray)
            it.toByteArray()
        }

        val actual = ByteArrayOutputStream().use {
            data.writeTo(it)
            it.toByteArray()
        }

        assertArrayEquals(expected, actual)
    }

    @Test
    fun decodePrimitiveDatumShortDefiniteLengthTest() {
        val dataArray = rand.nextBytes(127) // Short-Definite Length

        val expected = PrimitiveDatum().also {
            it.data = dataArray
        }
        val data = ByteArrayOutputStream().use {
            it.write(0x01)
            it.write(127)
            it.write(dataArray)
            it.toByteArray()
        }

        val actual = PrimitiveDatum().also { it.readFrom(data) }

        assertArrayEquals(expected.data, actual.data)
    }

    @Test
    fun encodePrimitiveDatumLongDefiniteLengthTest() {
        val dataArray = rand.nextBytes(128) // Short-Definite Length

        val data = PrimitiveDatum().also {
            it.data = dataArray
        }
        val expected = ByteArrayOutputStream().use {
            it.write(0x01)
            it.write(byteArrayOf(0x81.toByte(), 128.toByte()))
            it.write(dataArray)
            it.toByteArray()
        }

        val actual = ByteArrayOutputStream().use {
            data.writeTo(it)
            it.toByteArray()
        }

        assertArrayEquals(expected, actual)
    }

    @Test
    fun decodePrimitiveDatumLongDefiniteLengthTest() {
        val dataArray = rand.nextBytes(128) // Short-Definite Length

        val expected = PrimitiveDatum().also {
            it.data = dataArray
        }
        val data = ByteArrayOutputStream().use {
            it.write(0x01)
            it.write(byteArrayOf(0x81.toByte(), 128.toByte()))
            it.write(dataArray)
            it.toByteArray()
        }

        val actual = PrimitiveDatum().also { it.readFrom(data) }

        assertArrayEquals(expected.data, actual.data)
    }

    @Test
    fun decodePrimitiveDatumLongDefiniteLengthCorruptedTest() {
        val data = ByteArrayOutputStream().use {
            it.write(0x01)
            it.write(byteArrayOf(0x81.toByte()))
            it.toByteArray()
        }

        try {
            val actual = PrimitiveDatum().also { it.readFrom(data) }
            fail()
        } catch (exception: StreamCorruptedException) {
            println(exception)
        }
    }

    @Test
    fun encodePrimitiveDatumMultiByteTagTest() {
        val data = PrimitiveMultiBytesTagData().also {
            it.data1 = byteArrayOf(0x00)
            it.data2 = byteArrayOf(0x00)
        }
        val expected = byteArrayOf(
            0x01F, 0x01, 0x01, 0x00,
            0x01F, 0x81.toByte(), 0x01, 0x01, 0x00
        )

        val actual = ByteArrayOutputStream().use {
            data.writeTo(it)
            it.toByteArray()
        }

        assertArrayEquals(expected, actual)
    }

    @Test
    fun decodePrimitiveDatumMultiByteTagTest() {
        val expected = PrimitiveMultiBytesTagData().also {
            it.data1 = byteArrayOf(0x00)
            it.data2 = byteArrayOf(0x00)
        }
        val data = byteArrayOf(
            0x01F, 0x01, 0x01, 0x00,
            0x01F, 0x81.toByte(), 0x01, 0x01, 0x00
        )

        val actual = PrimitiveMultiBytesTagData().also { it.readFrom(data) }

        assertEquals(expected, actual)
    }

    @Test
    fun encodeSubPrimitiveDatumTest() {
        val data = SubPrimitiveDatum().also {
            it.data = byteArrayOf(0x00)
            it.data2 = byteArrayOf(0x01)
        }
        val expected = byteArrayOf(0x01, 0x01, 0x00, 0x02, 0x01, 0x01)

        val actual = ByteArrayOutputStream().use {
            data.writeTo(it)
            it.toByteArray()
        }

        assertArrayEquals(expected, actual)
    }

    @Test
    fun decodeSubPrimitiveDatumTest1() {
        val data = byteArrayOf(0x01, 0x01, 0x00, 0x02, 0x01, 0x01)
        val expected = SubPrimitiveDatum().also {
            it.data = byteArrayOf(0x00)
            it.data2 = byteArrayOf(0x01)
        }

        val actual = SubPrimitiveDatum().also { it.readFrom(data) }

        assertArrayEquals(expected.data, actual.data)
        assertArrayEquals(expected.data2, actual.data2)
    }

    @Test
    fun decodeSubPrimitiveDatumTest2() {
        val data = byteArrayOf(0x01, 0x01, 0x00, 0x02, 0x01, 0x01)
        val expected = SubPrimitiveDatum().also {
            it.data = byteArrayOf(0x00)
            it.data2 = byteArrayOf(0x01)
        }

        val actual = PrimitiveDatum().also { it.readFrom(data) }

        assertArrayEquals(expected.data, actual.data)
    }
}
