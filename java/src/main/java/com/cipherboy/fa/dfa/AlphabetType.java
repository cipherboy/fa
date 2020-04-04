package com.cipherboy.fa.dfa;

public enum AlphabetType {
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

    private AlphabetType(byte identifier) {
        this.identifier = identifier;
    }

    public byte getIdentifier() {
        return identifier;
    }

    public AlphabetType valueOf(byte identifier) {
        for (AlphabetType alpha : AlphabetType.values()) {
            if (alpha.identifier == identifier) {
                return alpha;
            }
        }

        return null;
    }
}
