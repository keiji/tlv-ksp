package dev.keiji.tlv.sample.foo

import dev.keiji.tlv.BerTlv
import dev.keiji.tlv.BerTlvItem
import dev.keiji.tlv.sample.bar.BerBar

@BerTlv
class BerFoo(
    @BerTlvItem([0xFA.toByte()])
    var bar: BerBar? = null,
)
