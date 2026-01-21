package dev.keiji.tlv.sample

import dev.keiji.tlv.sample.sub.SubpackagePrimitiveDatum
import dev.keiji.tlv.sample.sub.SubpackagePrimitiveOtherNested
import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayOutputStream
import kotlin.random.Random

class ConstructedDataTest {

    val serialized = byteArrayOf(
        0x02, 0x0C, // tag: [002], length: 12 bytes
        0x1F, 0x01, 0x02, 0x01, 0x02,
        0x1F, 0x81.toByte(), 0x01, 0x03, 0x04, 0x05, 0x06,
        0x1F, 0x02, 0x01, 0x07,
        0x1F, 0x03, 0x01, 0xFF.toByte(),
        0x1F, 0x81.toByte(), 0x03, 0x06, 0x48, 0x65, 0x6C, 0x6C, 0x6F, 0x21,
        0x03, 0x01, 0x7F,
        0x11, 0x10, // tag: [011], length: 10 bytes
        0x01, 0x03, 0x10, 0x20, 0x30,
        0x12, 0x02, // tag: [012], length: 2 bytes
        0x40, 0x50,
        0x30, 0x05, // tag: [030], length: 5 bytes
        0x21, 0x03, // tag: [021], length: 3 bytes
        0x31, 0x01, 0xFF.toByte(), // tag: [031], length: 1 byte, value: 0xFF
        0x30, 0x02, // tag: [030], length: 2 bytes
        0x31, 0x00,  // tag: [031], length: 0 bytes,
    )

    val deserialized = ConstructedData().also {
        it.structured = PrimitiveMultiBytesTagData(
            data1 = byteArrayOf(0x01, 0x02),
            data2 = byteArrayOf(0x04, 0x05, 0x06)
        )
        it.data1 = byteArrayOf(0x07)
        it.data2 = true
        it.data3 = "Hello!"
        it.data4 = 0x07F
        it.data5 = SubpackagePrimitiveDatum().also { sub ->
            sub.data = byteArrayOf(0x10, 0x20, 0x30)
            sub.data2 = byteArrayOf(0x40, 0x50)
            sub.data3 = SubpackagePrimitiveDatum.Nested().apply {
                value = SubpackagePrimitiveDatum.Nested.DeepNested().apply {
                    deepValue = byteArrayOf(0xFF.toByte())
                }
            }
        }
        it.data6 = null
        it.data7 = SubpackagePrimitiveOtherNested.SomeNested().apply {
            value = byteArrayOf()
        }
    }

    @Test
    fun encodeConstructedDataTest() {
        val data = deserialized.also {
            it.ignored = Random.nextBytes(10)
        }

        val actual = ByteArrayOutputStream().use {
            data.writeTo(it)
            it.toByteArray()
        }

        println(actual.toHex(", "))
        Assert.assertArrayEquals(serialized, actual)
    }

    @Test
    fun decodeConstructedDataTest() {
        val actual = ConstructedData().also { it.readFrom(serialized) }
        Assert.assertEquals(deserialized, actual)
    }

}

internal fun ByteArray.toHex(delimiter: String) = this.joinToString(delimiter) { "0x${it.toHex()}" }

internal fun Byte.toHex() = "%02x".format(this).uppercase()
