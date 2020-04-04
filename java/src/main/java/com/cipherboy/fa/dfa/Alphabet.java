package com.cipherboy.fa.dfa;

public enum Alphabet {
    ASCII ((byte) 0x01),
    UTF8 ((byte) 0x02),
    UTF16 ((byte) 0x03),
    UTF32 ((byte) 0x04),
    UInt8 ((byte) 0x05),
    UInt16 ((byte) 0x06),
    UInt32 ((byte) 0x07),
    UInt64 ((byte) 0x08),
    Fixed ((byte) 0x09),
    Variable ((byte) 0x0A);

    private byte identifier;

    private Alphabet(byte identifier) {
        this.identifier = identifier;
    }

    public byte getIdentifier() {
        return identifier;
    }

    public Alphabet valueOf(byte identifier) {
        for (Alphabet alpha : Alphabet.values()) {
            if (alpha.identifier == identifier) {
                return alpha;
            }
        }

        return null;
    }
}
