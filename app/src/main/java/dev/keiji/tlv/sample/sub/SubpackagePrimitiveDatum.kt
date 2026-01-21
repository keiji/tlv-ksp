package dev.keiji.tlv.sample.sub

import dev.keiji.tlv.BerTlv
import dev.keiji.tlv.BerTlvItem
import dev.keiji.tlv.sample.PrimitiveDatum

@BerTlv
class SubpackagePrimitiveDatum : PrimitiveDatum() {
    @BerTlvItem(tag = [0x12], order = 100)
    var data2: ByteArray? = null

    @BerTlvItem(tag = [0x30], order = 110)
    var data3: Nested? = null

    @BerTlv
    class Nested(
        @BerTlvItem(tag = [0x21], order = 10)
        var value: DeepNested? = null
    ) {

        @BerTlv
        class DeepNested(
            @BerTlvItem(tag = [0x31], order = 10)
            var deepValue: ByteArray? = null
        )
    }
}
