package dev.keiji.tlv.sample

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream
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
    fun encodePrimitiveDatumShortDefiniteLengthTest() {
        val dataArray  = rand.nextBytes(127) // Short-Definite Length

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
        val dataArray  = rand.nextBytes(127) // Short-Definite Length

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
        val dataArray  = rand.nextBytes(128) // Short-Definite Length

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
        val dataArray  = rand.nextBytes(128) // Short-Definite Length

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

}
