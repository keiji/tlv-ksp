package dev.keiji.tlv.sample.issue.gh218

import dev.keiji.tlv.sample.issue.gh218.standalone.BasicDatum
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.io.ByteArrayOutputStream

/**
 * `readFrom` and `writeTo` is not declared in `dev.keiji.tlv.sample.issue.gh218.standalone`
 *  package as extension function so it should not be imported, otherwise it causes compile error
 */
class Gh218Test {

    val berBytes = byteArrayOf(
        0x0F.toByte(), 0x03.toByte(),
        0x01.toByte(), 0x02.toByte(), 0x03.toByte()
    )

    val berDatum = BerCompositeDatum().apply {
        basicDatum = BasicDatum().apply {
            data = byteArrayOf(0x01.toByte(), 0x02.toByte(), 0x03.toByte())
        }
    }

    val compactBytes = byteArrayOf(
        0xF3.toByte(),
        0x01.toByte(), 0x02.toByte(), 0x03.toByte()
    )
    val compactDatum = CompactCompositeDatum().apply {
        basicDatum = BasicDatum().apply {
            data = byteArrayOf(0x01.toByte(), 0x02.toByte(), 0x03.toByte())
        }
    }

    @Test
    fun `should compile BerTlvDecoder without readFrom declared in standalone package`() {
        val results = BerCompositeDatum().apply { readFrom(berBytes) }

        assertArrayEquals(berDatum.basicDatum!!.data!!, results.basicDatum!!.data!!)
    }

    @Test
    fun `should compile BerTlvEncoder without writeTo declared in standalone package`() {
        val results = ByteArrayOutputStream().use {
            berDatum.writeTo(it)
            it.toByteArray()
        }

        assertArrayEquals(berBytes, results)
    }

    @Test
    fun `should compile CompactTlvDecoder without readFrom declared in standalone package`() {
        val results = CompactCompositeDatum().apply { readFrom(compactBytes) }

        assertArrayEquals(compactDatum.basicDatum!!.data!!, results.basicDatum!!.data!!)
    }

    @Test
    fun `should compile CompactTlvEncoder without writeTo declared in standalone package`() {
        val results = ByteArrayOutputStream().use {
            compactDatum.writeTo(it)
            it.toByteArray()
        }

        assertArrayEquals(compactBytes, results)
    }
}
