package dev.keiji.tlv

import org.junit.Assert
import org.junit.Test

class BerTlvEncoderProcessorTest {

    @Test
    fun validateAnnotation1() {
        BerTlvEncoderProcessor.validateAnnotation(byteArrayOf(0x6E))
    }

    @Test
    fun validateAnnotation2() {
        BerTlvEncoderProcessor.validateAnnotation(byteArrayOf(0x5F, 0x6E))
    }

    @Test
    fun validateAnnotation3() {
        BerTlvEncoderProcessor.validateAnnotation(
            byteArrayOf(
                0x5F,
                0xFF.toByte(),
                0x81.toByte(),
                0x01
            )
        )
    }

    @Test
    fun validateAnnotation_exception1() {
        try {
            BerTlvEncoderProcessor.validateAnnotation(byteArrayOf(0x4F, 0x01))
            Assert.fail()
        } catch (exception: IllegalArgumentException) {
            println(exception)
        }
    }

    @Test
    fun validateAnnotation_exception2() {
        try {
            BerTlvEncoderProcessor.validateAnnotation(byteArrayOf(0x5F))
            Assert.fail()
        } catch (exception: IllegalArgumentException) {
            println(exception)
        }
    }

    @Test
    fun validateAnnotation_exception3() {
        try {
            BerTlvEncoderProcessor.validateAnnotation(byteArrayOf(0x5F, 0x80.toByte()))
            Assert.fail()
        } catch (exception: IllegalArgumentException) {
            println(exception)
        }
    }

    @Test
    fun validateAnnotation_exception4() {
        try {
            BerTlvEncoderProcessor.validateAnnotation(byteArrayOf(0x5F, 0x71, 0x01))
            Assert.fail()
        } catch (exception: IllegalArgumentException) {
            println(exception)
        }
    }
}
