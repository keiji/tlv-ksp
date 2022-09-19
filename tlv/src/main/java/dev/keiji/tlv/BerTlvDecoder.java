package dev.keiji.tlv;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.StreamCorruptedException;
import java.math.BigInteger;

public class BerTlvDecoder {
    private static int MASK_MSB_BITS = 0b100_00000;
    private static int MASK_PC_BITS = 0b00_1_00000;
    private static int MASK_TAG_BITS = 0b00_0_11111;
    private static byte MASK_TAG_FULL_BITS = 0b00_0_11111;

    private static int PC_PRIMITIVE = 0b0;
    private static int PC_CONSTRUCTED = 0b1;

    private static int MAX_LENGTH_FILED_LENGTH = 126;

    public interface Callback {
        void onLargeItemDetected(
                byte[] tag,
                BigInteger length,
                InputStream inputStream
        );

        void onItemDetected(
                byte[] tag,
                byte[] data
        );
    }

    public static void readFrom(
            InputStream inputStream,
            Callback callback
    ) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("inputStream must not be null.");
        }
        if (callback == null) {
            throw new IllegalArgumentException("callback must not be null.");
        }

        while (true) {
            byte[] tag = readTag(inputStream);
            if (tag == null) {
                break;
            }

            BigInteger length = readLength(inputStream);

            boolean isLargeItem = length.bitLength() > (Integer.SIZE - 1);
            if (!isLargeItem) {
                byte[] data = readData(inputStream, length);
                callback.onItemDetected(tag, data);
            } else {
                callback.onLargeItemDetected(tag, length, inputStream);
            }
        }

    }

    static byte[] readTag(InputStream inputStream) throws IOException {
        ByteArrayOutputStream tagStream = new ByteArrayOutputStream();

        while (true) {
            int b = inputStream.read();
            if (b < 0) {
                return null;
            }

            byte tagBits = (byte) (b & MASK_TAG_BITS);

            int tagSize = tagStream.size();

            if (tagSize == 0) {
                if (tagBits != MASK_TAG_FULL_BITS) {
                    return new byte[]{(byte) b};
                }
                tagStream.write(new byte[]{(byte) b});
            } else {
                tagStream.write(new byte[]{(byte) b});
                if ((b & MASK_MSB_BITS) != 0) {
                    continue;
                }
                return tagStream.toByteArray();
            }
        }
    }

    static BigInteger readLength(InputStream inputStream) throws IOException {
        int b = inputStream.read();
        if (b < 0) {
            throw new StreamCorruptedException();
        }

        boolean longDef = (b & MASK_MSB_BITS) != 0;

        if (!longDef) {
            return BigInteger.valueOf(b);
        } else {
            int fieldLength = (b ^ MASK_MSB_BITS);
            if (fieldLength > MAX_LENGTH_FILED_LENGTH) {
                throw new InvalidObjectException("Long Definite length must not be grater 126 bytes.");
            }

            byte[] lengthBytes = new byte[fieldLength];

            int offset = 0;

            while (true) {
                int readLength = inputStream.read(lengthBytes, offset, (fieldLength - offset));
                if (readLength < 0) {
                    throw new StreamCorruptedException();
                } else if (readLength < (fieldLength - offset)) {
                    offset += readLength;
                } else {
                    break;
                }
            }
            return new BigInteger(+1, lengthBytes);
        }
    }

    static byte[] readData(InputStream inputStream, BigInteger length) throws IOException {
        int dataLength = length.intValue();
        byte[] data = new byte[dataLength];
        int offset = 0;

        while (true) {
            int readLength = inputStream.read(data, offset, (dataLength - offset));
            if (readLength < 0) {
                throw new StreamCorruptedException();
            } else if (readLength < (dataLength - offset)) {
                offset += readLength;
            } else {
                return data;
            }
        }
    }
}
