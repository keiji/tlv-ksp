package dev.keiji.tlv.sample.sub

import dev.keiji.tlv.CompactTlv
import dev.keiji.tlv.CompactTlvItem

@CompactTlv
data class SubpackageCompactPrimitiveDatum(
    @CompactTlvItem(tag = 0x0C)
    var data1: ByteArray? = null,

    @CompactTlvItem(tag = 0x0D)
    var data2: ByteArray? = null,

    @CompactTlvItem(tag = 0x0E)
    var data3: Nested? = null
) {
    @CompactTlv
    data class Nested(
        @CompactTlvItem(tag = 0x0F)
        var value: ByteArray? = null
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Nested

            return value.contentEquals(other.value)
        }

        override fun hashCode(): Int {
            return value?.contentHashCode() ?: 0
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SubpackageCompactPrimitiveDatum

        if (!data1.contentEquals(other.data1)) return false
        if (!data2.contentEquals(other.data2)) return false
        if (data3 != other.data3) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data1?.contentHashCode() ?: 0
        result = 31 * result + (data2?.contentHashCode() ?: 0)
        result = 31 * result + (data3?.hashCode() ?: 0)
        return result
    }
}
