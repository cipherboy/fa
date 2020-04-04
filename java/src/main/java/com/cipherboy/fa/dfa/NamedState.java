package com.cipherboy.fa.dfa;

public final class NamedState extends StateData {
    private byte[][] names;

    public NamedState() {
        setType(DataType.Named);
    }

    public NamedState(byte[][] names) {
        this();

        setNames(names);
    }

    public byte[][] getNames() {
        return names;
    }

    public byte[] getName(long id) {
        assert(id < names.length);
        return names[(int) id];
    }

    public void setNames(byte[][] names) {
        this.names = names;
    }
}
