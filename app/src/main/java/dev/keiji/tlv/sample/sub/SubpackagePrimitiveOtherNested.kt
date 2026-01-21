package dev.keiji.tlv.sample.sub

import dev.keiji.tlv.BerTlv
import dev.keiji.tlv.BerTlvItem

class SubpackagePrimitiveOtherNested {

    @BerTlv
    data class SomeNested(
        @BerTlvItem(tag = [0x31], order = 10)
        var value: ByteArray? = null
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as SomeNested

            return value.contentEquals(other.value)
        }

        override fun hashCode(): Int {
            return value?.contentHashCode() ?: 0
        }
    }
}
