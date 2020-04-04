package com.cipherboy.fa.dfa;

public enum StateType {
    Unnamed ((byte) 0x01),
    Named ((byte) 0x02);

    private byte identifier;

    private StateType(byte identifier) {
        this.identifier = identifier;
    }

    public byte getIdentifier() {
        return identifier;
    }

    public StateType valueOf(byte identifier) {
        for (StateType state : StateType.values()) {
            if (state.identifier == identifier) {
                return state;
            }
        }

        return null;
    }
}
