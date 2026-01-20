package dev.keiji.tlv.sample.bar

import dev.keiji.tlv.BerTlv
import dev.keiji.tlv.BerTlvItem

@BerTlv
class BerBar(
    @BerTlvItem(tag = [0xFB.toByte()])
    var value: ByteArray? = null
)
