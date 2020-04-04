package com.cipherboy.fa.dfa;

public enum TerminationType {
    RejectStates ((byte) 0x01),
    AcceptStates ((byte) 0x02);

    private byte identifier;

    private Termination(byte identifier) {
        this.identifier = identifier;
    }

    public byte getIdentifier() {
        return identifier;
    }

    public Termination valueOf(byte identifier) {
        for (Termination termination : Termination.values()) {
            if (termination.identifier == identifier) {
                return termination;
            }
        }

        return null;
    }
}
