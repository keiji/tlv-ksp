package dev.keiji.tlv.sample

import dev.keiji.tlv.CompactTlv
import dev.keiji.tlv.CompactTlvItem
import dev.keiji.tlv.sample.sub.SubpackageCompactPrimitiveDatum

@CompactTlv
open class CompactPrimitiveDatum {

    @CompactTlvItem(tag = 0x01, order = 10)
    var data1: ByteArray? = null

    @CompactTlvItem(tag = 0x02, order = 5)
    var data2: ByteArray? = null

    @CompactTlvItem(tag = 0x03, order = 100)
    var data3: SubpackageCompactPrimitiveDatum? = null
}
