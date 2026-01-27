package dev.keiji.tlv.sample.issue.gh218

import dev.keiji.tlv.CompactTlv
import dev.keiji.tlv.CompactTlvItem
import dev.keiji.tlv.sample.issue.gh218.standalone.BasicDatum

@CompactTlv
class CompactCompositeDatum(
    @CompactTlvItem(tag = 0x0F, typeConverter = BasicDatum.Converter::class)
    var basicDatum: BasicDatum? = null,
)
