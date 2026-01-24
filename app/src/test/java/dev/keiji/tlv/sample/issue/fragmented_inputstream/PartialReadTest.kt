package dev.keiji.tlv.sample.issue.fragmented_inputstream

import org.junit.Assert.assertArrayEquals
import org.junit.Ignore
import org.junit.Test
import java.io.ByteArrayInputStream

class PartialReadTest {
    @Ignore("Enable this test after fixing the issue.")
    @Test
    fun testBerPartialReadHandling() {
        val expectedValue = ByteArray(10) { it.toByte() }

        val tlvData = byteArrayOf(0x01.toByte(), 0x0A.toByte()) + expectedValue

        val inputStream = FragmentedInputStream(
            ByteArrayInputStream(tlvData),
            chunkSize = 1
        )

        val resultObj = inputStream.use {
            PrimitiveDatum().also {
                it.readFrom(inputStream)
            }
        }

        assertArrayEquals(expectedValue, resultObj.data)
    }

    @Ignore("Enable this test after fixing the issue.")
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
