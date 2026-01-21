package dev.keiji.tlv.sample.sub

import dev.keiji.tlv.BerTlv
import dev.keiji.tlv.BerTlvItem

class SubpackagePrimitiveOtherNested {

    @BerTlv
    class SomeNested(
        @BerTlvItem(tag = [0x31], order = 10)
        var value: ByteArray? = null
    )
}
