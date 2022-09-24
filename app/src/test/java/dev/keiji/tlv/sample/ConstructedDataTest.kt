package dev.keiji.tlv.sample

import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayOutputStream

class ConstructedDataTest {

    @Test
    fun encodeConstructedDataTest() {
        val data = ConstructedData().also {
            it.structured = PrimitiveMultiBytesTagData(
                data1 = byteArrayOf(0x01, 0x02),
                data2 = byteArrayOf(0x04, 0x05, 0x06)
            )
            it.data1 = byteArrayOf(0x07)
            it.data2 = true
            it.data3 = "Hello!"
        }

        val expected = byteArrayOf(
            0x02, 0x0C, // tag: [002], length: 12 bytes
            0x1F, 0x01, 0x02, 0x01, 0x02,
            0x1F, 0x81.toByte(), 0x01, 0x03, 0x04, 0x05, 0x06,
            0x1F, 0x02, 0x01, 0x07,
            0x1F, 0x03, 0x01, 0xFF.toByte(),
            0x1F, 0x81.toByte(), 0x03, 0x06, 0x48, 0x65, 0x6C, 0x6C, 0x6F, 0x21,
        )

        val actual = ByteArrayOutputStream().use {
            data.writeTo(it)
            it.toByteArray()
        }

        println(actual.toHex(", "))
        Assert.assertArrayEquals(expected, actual)
    }

    @Test
    fun decodeConstructedDataTest() {
        val data = byteArrayOf(
            0x02, 0x0C, // tag: [002], length: 12 bytes
            0x1F, 0x01, 0x02, 0x01, 0x02,
            0x1F, 0x81.toByte(), 0x01, 0x03, 0x04, 0x05, 0x06,
            0x1F, 0x02, 0x01, 0x07,
            0x1F, 0x03, 0x01, 0xFF.toByte(),
            0x1F, 0x81.toByte(), 0x03, 0x06, 0x48, 0x65, 0x6C, 0x6C, 0x6F, 0x21,
        )

        val expected = ConstructedData().also {
            it.structured = PrimitiveMultiBytesTagData(
                data1 = byteArrayOf(0x01, 0x02),
                data2 = byteArrayOf(0x04, 0x05, 0x06)
            )
            it.data1 = byteArrayOf(0x07)
            it.data2 = true
            it.data3 = "Hello!"
        }

        val actual = ConstructedData().also { it.readFrom(data) }
        Assert.assertEquals(expected, actual)
    }

}

internal fun ByteArray.toHex(delimiter: String) = this.joinToString(delimiter) { "0x${it.toHex()}" }

internal fun Byte.toHex() = "%02x".format(this).uppercase()
