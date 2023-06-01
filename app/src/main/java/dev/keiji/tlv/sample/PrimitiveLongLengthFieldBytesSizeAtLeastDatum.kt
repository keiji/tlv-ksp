package dev.keiji.tlv.sample

import dev.keiji.tlv.BerTlv
import dev.keiji.tlv.BerTlvItem

@BerTlv
open class PrimitiveLongLengthFieldBytesSizeAtLeastDatum {

    @BerTlvItem(tag = [0x01], order = 10, longDefLengthFieldSizeAtLeast = 2)
    var data: ByteArray? = null
}
