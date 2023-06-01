package dev.keiji.tlv.sample

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.StreamCorruptedException
import kotlin.random.Random

class PrimitiveLengthLongFieldBytesSizeDataTest {
    private val rand = Random(System.currentTimeMillis())

    @Test
    fun encodePrimitiveDatumTest() {
        val data = PrimitiveLongLengthFieldBytesSizeAtLeastDatum().also {
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
        val expected = PrimitiveLongLengthFieldBytesSizeAtLeastDatum().also {
            it.data = byteArrayOf(0x00)
        }

        val actual = PrimitiveDatum().also { it.readFrom(data) }

        assertArrayEquals(expected.data, actual.data)
    }

    @Test
    fun decodePrimitiveDatumCorruptedTest() {
        val data = byteArrayOf(0x01, 0x01)

        try {
            val actual = PrimitiveLongLengthFieldBytesSizeAtLeastDatum().also { it.readFrom(data) }
            fail()
        } catch (exception: StreamCorruptedException) {
            println(exception)
        }
    }

    @Test
    fun encodePrimitiveDatumShortDefiniteLengthTest() {
        val dataArray = rand.nextBytes(127) // Short-Definite Length

        val data = PrimitiveLongLengthFieldBytesSizeAtLeastDatum().also {
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

        val expected = PrimitiveLongLengthFieldBytesSizeAtLeastDatum().also {
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
        val dataArray = rand.nextBytes(128) // Long-Definite Length

        val data = PrimitiveLongLengthFieldBytesSizeAtLeastDatum().also {
            it.data = dataArray
        }
        val expected = ByteArrayOutputStream().use {
            it.write(0x01)
            it.write(byteArrayOf(0x82.toByte(), 0x00.toByte(), 128.toByte()))
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
        val dataArray = rand.nextBytes(128) // Long-Definite Length

        val expected = PrimitiveLongLengthFieldBytesSizeAtLeastDatum().also {
            it.data = dataArray
        }
        val data = ByteArrayOutputStream().use {
            it.write(0x01)
            it.write(byteArrayOf(0x82.toByte(), 0x00.toByte(), 128.toByte()))
            it.write(dataArray)
            it.toByteArray()
        }

        val actual = PrimitiveLongLengthFieldBytesSizeAtLeastDatum().also { it.readFrom(data) }

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
            val actual = PrimitiveLongLengthFieldBytesSizeAtLeastDatum().also { it.readFrom(data) }
            fail()
        } catch (exception: StreamCorruptedException) {
            println(exception)
        }
    }
}
