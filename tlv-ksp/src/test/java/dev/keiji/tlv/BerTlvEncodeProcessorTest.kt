package dev.keiji.tlv

import dev.keiji.tlv.BerTlvEncoderProcessor.Companion.validateAnnotation
import junit.framework.Assert.fail
import org.junit.Test

class BerTlvEncodeProcessorTest {

    @Test
    fun validateAnnotation1() {
        validateAnnotation(byteArrayOf(0x6E))
    }

    @Test
    fun validateAnnotation2() {
        validateAnnotation(byteArrayOf(0x5F, 0x6E))
    }

    @Test
    fun validateAnnotation3() {
        validateAnnotation(byteArrayOf(0x5F, 0xFF.toByte(), 0x81.toByte(), 0x01))
    }

    @Test
    fun validateAnnotation_exception1() {
        try {
            validateAnnotation(byteArrayOf(0x4F, 0x01))
            fail()
        } catch (exception: IllegalArgumentException) {
            println(exception)
        }
    }

    @Test
    fun validateAnnotation_exception2() {
        try {
            validateAnnotation(byteArrayOf(0x5F))
            fail()
        } catch (exception: IllegalArgumentException) {
            println(exception)
        }
    }

    @Test
    fun validateAnnotation_exception3() {
        try {
            validateAnnotation(byteArrayOf(0x5F, 0x80.toByte()))
            fail()
        } catch (exception: IllegalArgumentException) {
            println(exception)
        }
    }

    @Test
    fun validateAnnotation_exception4() {
        try {
            validateAnnotation(byteArrayOf(0x5F, 0x71, 0x01))
            fail()
        } catch (exception: IllegalArgumentException) {
            println(exception)
        }
    }
}
