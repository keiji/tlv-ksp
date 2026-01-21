package dev.keiji.tlv.sample.sub

import dev.keiji.tlv.CompactTlv
import dev.keiji.tlv.CompactTlvItem

@CompactTlv
class SubpackageCompactPrimitiveDatum {
    @CompactTlvItem(tag = 0x0C)
    var data1: ByteArray? = null

    @CompactTlvItem(tag = 0x0D)
    var data2: ByteArray? = null

    @CompactTlvItem(tag = 0x0E)
    var data3: Nested? = null

    @CompactTlv
    class Nested {
        @CompactTlvItem(tag = 0x0F)
        var value: ByteArray? = null
    }
}
