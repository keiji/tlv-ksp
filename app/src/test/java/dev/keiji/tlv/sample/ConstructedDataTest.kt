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
            it.data4 = 0x07F
            it.data5 = arrayListOf(0x02, 0x03)
            it.data6 = arrayListOf(
                PrimitiveMultiBytesTagData(
                    data1 = byteArrayOf(0x07, 0x08),
                    data2 = byteArrayOf(0x09, 0x0A, 0x0B)
                ),
                PrimitiveMultiBytesTagData(
                    data1 = byteArrayOf(0x0C, 0x0D),
                    data2 = byteArrayOf(0x0E, 0x0F, 0x10)
                ),
            )
        }

        val expected = byteArrayOf(
            0x02, 0x0C, // tag: [002], length: 12 bytes
            0x1F, 0x01, 0x02, 0x01, 0x02,
            0x1F, 0x81.toByte(), 0x01, 0x03, 0x04, 0x05, 0x06,
            0x1F, 0x02, 0x01, 0x07,
            0x1F, 0x03, 0x01, 0xFF.toByte(),
            0x1F, 0x81.toByte(), 0x03, 0x06, 0x48, 0x65, 0x6C, 0x6C, 0x6F, 0x21,
            0x03, 0x01, 0x7F,
            0x05, 0x01, 0x02,
            0x05, 0x01, 0x03,
            0x06, 0x0C,
            0x1F, 0x01, 0x02, 0x07, 0x08,
            0x1F, 0x81.toByte(), 0x01, 0x03, 0x09, 0x0A, 0x0B,
            0x06, 0x0C, 0x1F, 0x01, 0x02, 0x0C, 0x0D,
            0x1F, 0x81.toByte(), 0x01, 0x03, 0x0E, 0x0F, 0x10,
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
            0x03, 0x01, 0x7F,
            0x05, 0x01, 0x02,
            0x05, 0x01, 0x03,
            0x06, 0x0C,
            0x1F, 0x01, 0x02, 0x07, 0x08,
            0x1F, 0x81.toByte(), 0x01, 0x03, 0x09, 0x0A, 0x0B,
            0x06, 0x0C, 0x1F, 0x01, 0x02, 0x0C, 0x0D,
            0x1F, 0x81.toByte(), 0x01, 0x03, 0x0E, 0x0F, 0x10,
        )

        val expected = ConstructedData().also {
            it.structured = PrimitiveMultiBytesTagData(
                data1 = byteArrayOf(0x01, 0x02),
                data2 = byteArrayOf(0x04, 0x05, 0x06)
            )
            it.data1 = byteArrayOf(0x07)
            it.data2 = true
            it.data3 = "Hello!"
            it.data4 = 0x7F
            it.data5 = arrayListOf(0x02, 0x03)
            it.data6 = arrayListOf(
                PrimitiveMultiBytesTagData(
                    data1 = byteArrayOf(0x07, 0x08),
                    data2 = byteArrayOf(0x09, 0x0A, 0x0B)
                ),
                PrimitiveMultiBytesTagData(
                    data1 = byteArrayOf(0x0C, 0x0D),
                    data2 = byteArrayOf(0x0E, 0x0F, 0x10)
                ),
            )
        }

        val actual = ConstructedData().also { it.readFrom(data) }
        Assert.assertEquals(expected, actual)
    }

}

internal fun ByteArray.toHex(delimiter: String) = this.joinToString(delimiter) { "0x${it.toHex()}" }

internal fun Byte.toHex() = "%02x".format(this).uppercase()
