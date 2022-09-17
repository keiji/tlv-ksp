package dev.keiji.tlv.sample

import dev.keiji.tlv.BerTlv
import dev.keiji.tlv.BerTlvItem

@BerTlv
data class PrimitiveMultiBytesTagData(
    @BerTlvItem(tag = [0x01F, 0x01])
    var data1: ByteArray? = null,

    @BerTlvItem(tag = [0x01F, 0x81.toByte(), 0x01])
    var data2: ByteArray? = null

) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PrimitiveMultiBytesTagData

        if (data1 != null) {
            if (other.data1 == null) return false
            if (!data1.contentEquals(other.data1)) return false
        } else if (other.data1 != null) return false
        if (data2 != null) {
            if (other.data2 == null) return false
            if (!data2.contentEquals(other.data2)) return false
        } else if (other.data2 != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data1?.contentHashCode() ?: 0
        result = 31 * result + (data2?.contentHashCode() ?: 0)
        return result
    }
}
