package dev.keiji.tlv.sample

import dev.keiji.tlv.sample.bar.CompactBar
import dev.keiji.tlv.sample.foo.CompactFoo
import dev.keiji.tlv.sample.foo.readFrom
import dev.keiji.tlv.sample.foo.writeTo
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Random

class CompactCrossPackageNestingTest {

    private val r = Random()

    @Test
    fun compactFoo_compactBar_test() {
        val compactValue = ByteArray(10).also { r.nextBytes(it) }

        val bar = CompactBar().apply {
            value = compactValue
        }
        val foo = CompactFoo().apply {
            this.bar = bar
        }

        // Encode
        val encoded = ByteArrayOutputStream().let { stream ->
            foo.writeTo(stream)
            stream.toByteArray()
        }

        // [0x20|0x0B, 0x10|0x0A, [...compactValue...]]
        assertEquals(0x2B.toByte(), encoded[0])
        assertEquals(0x1A.toByte(), encoded[1])

        // Decode
        val decodedFoo = ByteArrayInputStream(encoded).use { inputStream ->
            CompactFoo().also { it.readFrom(inputStream) }
        }

        assertNotNull(decodedFoo.bar)
        assertArrayEquals(compactValue, decodedFoo.bar?.value)
    }
}
