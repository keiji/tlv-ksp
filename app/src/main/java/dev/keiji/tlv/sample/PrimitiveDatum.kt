package dev.keiji.tlv.sample

import dev.keiji.tlv.BerTlv
import dev.keiji.tlv.BerTlvItem

@BerTlv
class PrimitiveDatum {

    @BerTlvItem(tag = [0x01])
    var data: ByteArray? = null
}
