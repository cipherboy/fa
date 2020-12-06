package com.cipherboy.fa.dfa;

import java.util.Arrays;

public class IntegerLetter extends Letter {
    private int width;

    public IntegerLetter() {}

    public IntegerLetter(DFA instance) {
        super(instance);
    }

    public IntegerLetter(DFA instance, long id, int bytes) {
        super(instance, id, IntegerLetter.encode(id, bytes));
        this.width = bytes;
    }

    public static byte[] encode(long number, int bytes) throws IllegalArgumentException {
        if ((number < 0 && bytes != 8) || (number < (1 << bytes))) {
            String msg = "Number (" + number + ") exceeded capacity of ";
            msg += "storage size (" + bytes + " bytes).";
            throw new IllegalArgumentException(msg);
        }

        byte[] result = new byte[bytes];
        for (int i = 0; i < bytes; i++) {
            result[bytes - i - 1] = (byte) Math.abs(number & 0xff);
            number = number / 256;
        }
        return result;
    }

    public static long decode(byte[] data) throws IllegalArgumentException {
        long result = 0;
        for (byte doublenibble : data) {
            result *= 256;
            result += doublenibble;
        }

        return result;
    }
}
