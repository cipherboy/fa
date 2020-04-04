package com.cipherboy.fa.dfa;

public abstract class StateData {
    public enum DataType {
        Named
    };

    private DataType type;

    public DataType getType() {
        return type;
    }

    protected void setType(DataType type) {
        this.type = type;
    }
}
