package dev.keiji.tlv.sample

import dev.keiji.tlv.CompactTlv
import dev.keiji.tlv.CompactTlvItem

@CompactTlv
open class CompactPrimitiveDatum {

    @CompactTlvItem(tag = 0x01, order = 10)
    var data: ByteArray? = null
}
