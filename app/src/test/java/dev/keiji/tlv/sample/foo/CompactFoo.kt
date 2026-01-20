package dev.keiji.tlv.sample.foo

import dev.keiji.tlv.CompactTlv
import dev.keiji.tlv.CompactTlvItem
import dev.keiji.tlv.sample.bar.CompactBar

@CompactTlv
class CompactFoo(
    @CompactTlvItem(0x02.toByte())
    var bar: CompactBar? = null,
)
