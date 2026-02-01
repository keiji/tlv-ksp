package dev.keiji.tlv.sample.issue.gh218

import dev.keiji.tlv.BerTlv
import dev.keiji.tlv.BerTlvItem
import dev.keiji.tlv.sample.issue.gh218.standalone.BasicDatum

@BerTlv
class BerCompositeDatum(
    @BerTlvItem(tag = [0x0F], typeConverter = BasicDatum.Converter::class)
    var basicDatum: BasicDatum? = null,
)
