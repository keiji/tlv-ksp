package dev.keiji.tlv

@Target(AnnotationTarget.CLASS)
annotation class BerTlv

@Target(AnnotationTarget.FIELD)
annotation class BerTlvItem(val tag: ByteArray)
