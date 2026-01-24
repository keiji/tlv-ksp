package dev.keiji.tlv.sample.sub

import dev.keiji.tlv.BerTlv
import dev.keiji.tlv.BerTlvItem
import dev.keiji.tlv.sample.PrimitiveDatum

@BerTlv
data class SubpackagePrimitiveDatum(
    @BerTlvItem(tag = [0x12], order = 100)
    var data2: ByteArray? = null,

    @BerTlvItem(tag = [0x30], order = 110)
    var data3: Nested? = null
) : PrimitiveDatum() {
    @BerTlv
    data class Nested(
        @BerTlvItem(tag = [0x21], order = 10)
        var value: DeepNested? = null
    ) {

        @BerTlv
        data class DeepNested(
            @BerTlvItem(tag = [0x31], order = 10)
            var deepValue: ByteArray? = null
        ) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as DeepNested

                return deepValue.contentEquals(other.deepValue)
            }

            override fun hashCode(): Int {
                return deepValue?.contentHashCode() ?: 0
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SubpackagePrimitiveDatum

        if (!data2.contentEquals(other.data2)) return false
        if (data3 != other.data3) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data2?.contentHashCode() ?: 0
        result = 31 * result + (data3?.hashCode() ?: 0)
        return result
    }
}
