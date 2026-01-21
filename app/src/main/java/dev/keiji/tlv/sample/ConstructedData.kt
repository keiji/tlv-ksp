package dev.keiji.tlv.sample

import dev.keiji.tlv.BerTlv
import dev.keiji.tlv.BerTlvItem
import dev.keiji.tlv.BooleanTypeConverter
import dev.keiji.tlv.ByteTypeConverter
import dev.keiji.tlv.StringTypeConverter
import dev.keiji.tlv.sample.sub.SubpackagePrimitiveDatum
import dev.keiji.tlv.sample.sub.SubpackagePrimitiveOtherNested

@BerTlv
data class ConstructedData(
    @BerTlvItem(tag = [0x02])
    var structured: PrimitiveMultiBytesTagData? = null,

    @BerTlvItem(tag = [0x01F, 0x02])
    var data1: ByteArray? = null,

    @BerTlvItem(tag = [0x01F, 0x03], typeConverter = BooleanTypeConverter::class)
    var data2: Boolean? = null,

    @BerTlvItem(tag = [0x01F, 0x81.toByte(), 0x03], typeConverter = StringTypeConverter::class)
    var data3: String? = null,

    @BerTlvItem(tag = [0x03], typeConverter = ByteTypeConverter::class)
    var data4: Byte? = null,

    @BerTlvItem(tag = [0x11])
    var data5: SubpackagePrimitiveDatum? = null,

    @BerTlvItem(tag = [0x12])
    var data6: ByteArray? = null,

    @BerTlvItem(tag = [0x30])
    var data7: SubpackagePrimitiveOtherNested.SomeNested? = null,

    var ignored: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConstructedData

        if (data2 != other.data2) return false
        if (data4 != other.data4) return false
        if (structured != other.structured) return false
        if (!data1.contentEquals(other.data1)) return false
        if (data3 != other.data3) return false
        if (data5 != other.data5) return false
        if (!data6.contentEquals(other.data6)) return false
        if (data7 != other.data7) return false
        if (!ignored.contentEquals(other.ignored)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data2?.hashCode() ?: 0
        result = 31 * result + (data4 ?: 0)
        result = 31 * result + (structured?.hashCode() ?: 0)
        result = 31 * result + (data1?.contentHashCode() ?: 0)
        result = 31 * result + (data3?.hashCode() ?: 0)
        result = 31 * result + (data5?.hashCode() ?: 0)
        result = 31 * result + (data6?.contentHashCode() ?: 0)
        result = 31 * result + (data7?.hashCode() ?: 0)
        result = 31 * result + (ignored?.contentHashCode() ?: 0)
        return result
    }
}
