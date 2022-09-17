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
            it.data2 = byteArrayOf(0x08)
        }

        val expected = byteArrayOf(
            0x02, 0x0C.toByte(), // tag: [002], length: 12 bytes
            0x01F, 0x01, 0x02, 0x01, 0x02,
            0x01F, 0x81.toByte(), 0x01, 0x03, 0x04, 0x05, 0x06,
            0x01F, 0x02, 0x01, 0x07,
            0x1F, 0x81.toByte(), 0x03, 0x01, 0x08
        )

        val actual = ByteArrayOutputStream().use {
            data.writeTo(it)
            it.toByteArray()
        }

        Assert.assertArrayEquals(expected, actual)
    }

    @Test
    fun decodeConstructedDataTest() {
        val data = byteArrayOf(
            0x02, 0x0C.toByte(), // tag: [002], length: 12 bytes
            0x01F, 0x01, 0x02, 0x01, 0x02,
            0x01F, 0x81.toByte(), 0x01, 0x03, 0x04, 0x05, 0x06,
            0x01F, 0x02, 0x01, 0x07,
            0x1F, 0x81.toByte(), 0x03, 0x01, 0x08
        )

        val expected = ConstructedData().also {
            it.structured = PrimitiveMultiBytesTagData(
                data1 = byteArrayOf(0x01, 0x02),
                data2 = byteArrayOf(0x04, 0x05, 0x06)
            )
            it.data1 = byteArrayOf(0x07)
            it.data2 = byteArrayOf(0x08)
        }

        val actual = ConstructedData().also { it.readFrom(data) }
        Assert.assertEquals(expected, actual)
    }

}
