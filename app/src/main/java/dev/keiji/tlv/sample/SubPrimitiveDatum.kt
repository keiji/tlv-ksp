package dev.keiji.tlv.sample

import dev.keiji.tlv.BerTlv
import dev.keiji.tlv.BerTlvItem

@BerTlv
class SubPrimitiveDatum : PrimitiveDatum() {

    @BerTlvItem(tag = [0x02])
    var data2: ByteArray? = null
}
