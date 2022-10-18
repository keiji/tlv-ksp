package dev.keiji.tlv

import org.junit.Assert.fail
import org.junit.Test

class CompactTlvEncoderProcessorTest {
    @Test
    fun validateAnnotation1() {
        CompactTlvEncoderProcessor.validateAnnotation(0b0000)
    }

    @Test
    fun validateAnnotation2() {
        CompactTlvEncoderProcessor.validateAnnotation(0b1111)
    }

    @Test
    fun validateAnnotation_exception1() {
        try {
            CompactTlvEncoderProcessor.validateAnnotation(-1)
            fail()
        } catch (exception: IllegalArgumentException) {
            println(exception)
        }
    }

    @Test
    fun validateAnnotation_exception2() {
        try {
            CompactTlvEncoderProcessor.validateAnnotation(0b10000)
            fail()
        } catch (exception: IllegalArgumentException) {
            println(exception)
        }
    }
}
