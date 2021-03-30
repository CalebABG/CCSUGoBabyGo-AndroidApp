package com.calebabg.gobabygo;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Utils {

    // Converts 4 bytes (uint32_t) to an unsigned integer (Arduino is little endian)
    private static int composeUInt32(byte[] bytes, boolean isLittleEndian) {
        if (isLittleEndian)
            return ((bytes[3] << 24) + (bytes[2] << 16) + (bytes[1] << 8) + (bytes[0]));
        else
            return ((bytes[0] << 24) + (bytes[1] << 16) + (bytes[2] << 8) + (bytes[3]));
    }

    public static int[] getUnsignedBytes(byte[] bytes) {
        int bytesLen = bytes.length;

        int[] ints = new int[bytesLen];

        for (int i = 0; i < bytesLen; i++) ints[i] = toUnsignedInt(bytes[i]);

        return ints;
    }

    public static double map(double value, double min1, double max1, double min2, double max2) {
        return min2 + (max2 - min2) * ((value - min1) / (max1 - min1));
    }

    public static int toUnsignedInt(byte x) {
        return ((int) x) & 0xff;
    }

    public static  byte[] intToBytes(int myInteger){
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(myInteger).array();
    }

    public static double constrain(double amt, double low, double high) {
        return ((amt) < (low) ? (low) : (Math.min((amt), (high))));
    }
}
