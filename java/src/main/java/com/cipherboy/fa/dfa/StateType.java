package com.cipherboy.fa.dfa;

public enum StateType {
    Unnamed ((byte) 0x01),
    Named ((byte) 0x02);

    private byte identifier;

    private State(byte identifier) {
        this.identifier = identifier;
    }

    public byte getIdentifier() {
        return identifier;
    }

    public State valueOf(byte identifier) {
        for (State state : State.values()) {
            if (state.identifier == identifier) {
                return state;
            }
        }

        return null;
    }
}
