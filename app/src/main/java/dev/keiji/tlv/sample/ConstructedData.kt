package dev.keiji.tlv.sample

import dev.keiji.tlv.*

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

    @BerTlvItemList(tag = [0x03])
    var data5: ArrayList<ByteArray>? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConstructedData

        if (structured != other.structured) return false
        if (data1 != null) {
            if (other.data1 == null) return false
            if (!data1.contentEquals(other.data1)) return false
        } else if (other.data1 != null) return false
        if (data2 != other.data2) return false
        if (data3 != other.data3) return false
        if (data4 != other.data4) return false
        if (data5 != other.data5) return false

        return true
    }

    override fun hashCode(): Int {
        var result = structured?.hashCode() ?: 0
        result = 31 * result + (data1?.contentHashCode() ?: 0)
        result = 31 * result + (data2?.hashCode() ?: 0)
        result = 31 * result + (data3?.hashCode() ?: 0)
        result = 31 * result + (data4 ?: 0)
        result = 31 * result + (data5?.hashCode() ?: 0)
        return result
    }
}
