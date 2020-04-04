package com.cipherboy.fa.dfa;

public enum TerminationType {
    RejectStates ((byte) 0x01),
    AcceptStates ((byte) 0x02);

    private byte identifier;

    private TerminationType(byte identifier) {
        this.identifier = identifier;
    }

    public byte getIdentifier() {
        return identifier;
    }

    public TerminationType valueOf(byte identifier) {
        for (TerminationType termination : TerminationType.values()) {
            if (termination.identifier == identifier) {
                return termination;
            }
        }

        return null;
    }
}
