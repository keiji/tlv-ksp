package dev.keiji.tlv.sample.bar

import dev.keiji.tlv.CompactTlv
import dev.keiji.tlv.CompactTlvItem

@CompactTlv
class CompactBar(
    @CompactTlvItem(tag = 0x01.toByte())
    var value: ByteArray? = null
)
