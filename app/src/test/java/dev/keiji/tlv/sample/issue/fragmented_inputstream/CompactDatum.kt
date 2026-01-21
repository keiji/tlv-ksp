package dev.keiji.tlv.sample.issue.fragmented_inputstream

import dev.keiji.tlv.CompactTlv
import dev.keiji.tlv.CompactTlvItem

@CompactTlv
open class CompactDatum {

    @CompactTlvItem(tag = 0x01)
    var data: ByteArray? = null
}
