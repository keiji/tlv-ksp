package dev.keiji.tlv.sample

import dev.keiji.tlv.sample.bar.BerBar
import dev.keiji.tlv.sample.foo.BerFoo
import dev.keiji.tlv.sample.foo.readFrom
import dev.keiji.tlv.sample.foo.writeTo
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Random

class BerCrossPackageNestingTest {

    private val r = Random(0L)

    @Test
    fun berFoo_berBar_test() {
        val barValue = ByteArray(10).also { r.nextBytes(it) }

        val bar = BerBar().apply {
            value = barValue
        }
        val foo = BerFoo().apply {
            this.bar = bar
        }

        // Encode
        val encoded = ByteArrayOutputStream().let { stream ->
            foo.writeTo(stream)
            stream.toByteArray()
        }

        // [0xFA, 12, [0xFB, 10, ...barValue...]]
        assertEquals(0xFA.toByte(), encoded[0])
        assertEquals(12.toByte(), encoded[1])
        assertEquals(0xFB.toByte(), encoded[2])
        assertEquals(10.toByte(), encoded[3])

        // Decode
        val decodedFoo = ByteArrayInputStream(encoded).use { inputStream ->
            BerFoo().also { it.readFrom(inputStream) }
        }

        assertNotNull(decodedFoo.bar)
        assertArrayEquals(barValue, decodedFoo.bar?.value)
    }
}
