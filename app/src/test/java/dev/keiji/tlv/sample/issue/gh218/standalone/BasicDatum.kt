package dev.keiji.tlv.sample.issue.gh218.standalone

import dev.keiji.tlv.AbsTypeConverter

class BasicDatum(
    var data: ByteArray? = null
) {
    class Converter: AbsTypeConverter<BasicDatum>() {
        override fun convertFromByteArray(byteArray: ByteArray): BasicDatum {
            return BasicDatum(data = byteArray)
        }

        override fun convertToByteArray(data: BasicDatum): ByteArray {
            return data.data ?: ByteArray(0)
        }
    }
}
