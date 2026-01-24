package dev.keiji.tlv.sample.issue.fragmented_inputstream

import java.io.InputStream
import kotlin.math.min

class FragmentedInputStream(
    private val source: InputStream,
    private val chunkSize: Int = 1
) : InputStream() {

    override fun read(): Int {
        return source.read()
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        // Force a chunkSize smaller than the requested len.
        // This creates a situation where not all data has been read, even if more is available.
        val readLimit = min(len, chunkSize)
        return source.read(b, off, readLimit)
    }
}
