package dev.keiji.tlv;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.math.BigInteger;
import java.util.Random;

public class BerTlvDecoderTest {

    private Random rand = new Random(System.currentTimeMillis());

    @Test
    public void readTag1() throws IOException {
        byte[] data = new byte[100];
        rand.nextBytes(data);

        data[0] = 0x4F;
        InputStream inputStream = new ByteArrayInputStream(data);
        byte[] expected = new byte[]{0x4F};

        byte[] actual = BerTlvDecoder.readTag(inputStream);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void readTag2() throws IOException {
        byte[] data = new byte[100];
        rand.nextBytes(data);

        data[0] = 0x7F; // 01111111
        data[1] = 0x74;
        InputStream inputStream = new ByteArrayInputStream(data);
        byte[] expected = new byte[]{0x7F, 0x74};

        byte[] actual = BerTlvDecoder.readTag(inputStream);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void readTag3() throws IOException {
        byte[] data = new byte[100];
        rand.nextBytes(data);

        data[0] = 0x7F; // 01111111
        data[1] = (byte) 0x84;
        data[2] = 0x74;
        InputStream inputStream = new ByteArrayInputStream(data);
        byte[] expected = new byte[]{0x7F, (byte) 0x84, 0x74};

        byte[] actual = BerTlvDecoder.readTag(inputStream);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void readLength1() throws IOException {
        byte[] data = new byte[]{0b0_0000001};
        InputStream inputStream = new ByteArrayInputStream(data);
        BigInteger expected = BigInteger.ONE;

        BigInteger actual = BerTlvDecoder.readLength(inputStream);
        assertEquals(expected, actual);
    }

    @Test
    public void readLength2() throws IOException {
        byte[] data = new byte[]{(byte) 0b1_0000001, 127};
        InputStream inputStream = new ByteArrayInputStream(data);
        BigInteger expected = BigInteger.valueOf(127);

        BigInteger actual = BerTlvDecoder.readLength(inputStream);
        assertEquals(expected, actual);
    }

    @Test
    public void readLength3() throws IOException {
        byte[] data = new byte[]{(byte) 0b1_0000010, (byte) 0b11111111, 0x00000001, 0, 1, 2};
        InputStream inputStream = new ByteArrayInputStream(data);
        BigInteger expected = new BigInteger(+1, new byte[]{(byte) 0b11111111, 0x00000001});

        BigInteger actual = BerTlvDecoder.readLength(inputStream);
        assertEquals(expected, actual);
    }

    @Test
    public void readLength4_exception() throws IOException {
        byte[] data = new byte[128];
        rand.nextBytes(data);

        data[0] = (byte) (0b1_0000010 | 127);
        InputStream inputStream = new ByteArrayInputStream(data);
        try {
            BigInteger actual = BerTlvDecoder.readLength(inputStream);
            fail();
        } catch (InvalidObjectException exception) {
            System.out.println(exception);
        }
    }
}
