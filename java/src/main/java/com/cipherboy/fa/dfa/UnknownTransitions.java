package com.cipherboy.fa.dfa;

public enum UnknownTransitions {
    Reject ((byte) 0x01),
    Accept ((byte) 0x02),
    Goto ((byte) 0x03, GotoState.class);

    private byte identifier;
    private Class<? extends UnknownData> dataClass;

    private UnknownTransitions(byte identifier, Class<? extends UnknownData> dataClass) {
        this.identifier = identifier;
        this.dataClass = dataClass;
    }

    private UnknownTransitions(byte identifier) {
        this(identifier, null);
    }

    public byte getIdentifier() {
        return identifier;
    }

    public boolean hasAdditionalData() {
        return dataClass != null;
    }

    public Class<? extends UnknownData> getDataClass() {
        return dataClass;
    }

    public UnknownTransitions valueOf(byte identifier) {
        for (UnknownTransitions unkwn : UnknownTransitions.values()) {
            if (unkwn.identifier == identifier) {
                return unkwn;
            }
        }

        return null;
    }
}
