package dev.keiji.tlv;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;

public class BerTlvEncoder {

    public static void writeTo(
            byte[] tag,
            byte[] length,
            byte[] content,
            OutputStream os
    ) throws IOException {
        os.write(tag);
        os.write(length);
        os.write(content);
    }

    public static void writeTo(
            byte[] tag,
            byte[] content,
            OutputStream os
    ) throws IOException {
        if (content == null) {
            return;
        }

        os.write(tag);
        os.write(convertToLength(content));
        os.write(content);
    }

    static byte[] convertToLength(byte[] array) {
        return convertToLength(array.length);
    }

    static byte[] convertToLength(int size) {
        return convertToLength(BigInteger.valueOf(size));
    }

    static byte[] convertToLength(BigInteger size) {
        if (size.compareTo(BigInteger.ZERO) < 0) {
            throw new IllegalArgumentException("size must not be less than 0.");
        }

        if (size.bitLength() > 126 * 8) {
            throw new IllegalArgumentException("size length must not be greater or equal than 126 bytes.");
        }

        boolean isLongDef = size.bitLength() > 7;

        if (!isLongDef) {
            // Short definition
            return size.toByteArray();
        }

        // Long definition
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int bitLength = size.bitLength();
        bitLength += (bitLength % 8 == 0) ? 0 : 1;

        int sizeBits = 0b10000000 | (bitLength / 8);
        byte[] sizeBytes = size.toByteArray();
        int offset = (sizeBytes[0] == 0x00) ? 1 : 0;

        baos.write(sizeBits);
        baos.write(sizeBytes, offset, (sizeBytes.length - offset));
        baos.toByteArray();

        return baos.toByteArray();
    }
}
