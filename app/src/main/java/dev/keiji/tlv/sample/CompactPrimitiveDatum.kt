package dev.keiji.tlv.sample

import dev.keiji.tlv.CompactTlv
import dev.keiji.tlv.CompactTlvItem

@CompactTlv
open class CompactPrimitiveDatum {

    @CompactTlvItem(tag = 0x01, order = 10)
    var data1: ByteArray? = null

    @CompactTlvItem(tag = 0x02, order = 5)
    var data2: ByteArray? = null
}
