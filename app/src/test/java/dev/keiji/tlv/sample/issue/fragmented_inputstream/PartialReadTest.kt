package dev.keiji.tlv.sample.issue.fragmented_inputstream

import org.junit.Assert.assertArrayEquals
import org.junit.Ignore
import org.junit.Test
import java.io.ByteArrayInputStream

class PartialReadTest {
    @Test
    fun testBerPartialReadHandling() {
        val expectedValue = ByteArray(10) { it.toByte() }

        val tlvData = byteArrayOf(0x01.toByte(), 0x0A.toByte()) + expectedValue

        val inputStream = FragmentedInputStream(
            ByteArrayInputStream(tlvData),
            chunkSize = 1
        )

        val resultObj = inputStream.use { stream ->
            PrimitiveDatum().apply {
                readFrom(stream)
            }
        }

        assertArrayEquals(expectedValue, resultObj.data)
    }

    @Test
    fun testCompactPartialReadHandling() {
        val expectedValue = ByteArray(10) { it.toByte() }

        val tlvData = byteArrayOf(0x1A.toByte()) + expectedValue

        val inputStream = FragmentedInputStream(
            ByteArrayInputStream(tlvData),
            chunkSize = 1
        )

        val resultObj = inputStream.use { stream ->
            CompactDatum().apply {
                readFrom(stream)
            }
        }

        assertArrayEquals(expectedValue, resultObj.data)
    }
}
