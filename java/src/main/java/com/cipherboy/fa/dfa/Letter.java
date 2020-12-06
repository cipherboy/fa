package com.cipherboy.fa.dfa;

import java.util.Arrays;

public class Letter {
    private long identifier;
    private byte[] data;
    private DFA parent;

    public Letter() {}

    public Letter(DFA instance) {
        setDFA(instance);
    }

    public Letter(DFA instance, long id) {
        this(instance);

        setIdentifier(id);
    }

    public Letter(DFA instance, long id, byte[] repr) {
        this(instance, id);

        setRepresentation(repr);
    }

    protected void setDFA(DFA instance) {
        parent = instance;
    }

    public long getIdentifier() {
        return identifier;
    }

    public void setIdentifier(long id) {
        identifier = id;
    }

    public byte[] getRepresentation() {
        return data;
    }

    public void setRepresentation(byte[] repr) {
        data = Arrays.copyOf(repr, repr.length);
    }

    public boolean equals(byte[] other) {
        return Arrays.equals(data, other);
    }

    public boolean equals(Letter other) {
        return Arrays.equals(data, other.data);
    }
}
