package dev.keiji.tlv;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

public class BerTlvEncoderTest {
    @Test
    public void convertToLengthTest1() {
        byte[] expected = new byte[]{126};
        byte[] actual = BerTlvEncoder.convertToLength(126);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void convertToLengthTest2() {
        byte[] expected = new byte[] {(byte) 0b10000001, (byte) 128};
        byte[] actual = BerTlvEncoder.convertToLength(128);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void convertToLengthTest_Exception1() {
        try {
            byte[] actual = BerTlvEncoder.convertToLength(-1);
            fail();
        } catch (IllegalArgumentException exception) {
            System.out.println(exception);
        }
    }
}
