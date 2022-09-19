package dev.keiji.tlv;

class Utils {

    private Utils() {
    }

    static String toHex(byte[] byteArray, String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < byteArray.length; i++) {
            byte value = byteArray[i];
            String token = toHex(value);
            sb.append("0x").append(token);

            if (i < byteArray.length - 1) {
                sb.append(":");
            }
        }
        return sb.toString();
    }

    static String toHex(byte value) {
        return String.format("%02x", value).toUpperCase();
    }
}
