package com.cipherboy.fa.dfa;

public abstract class AlphabetData {
    public enum DataType {
        Bounded,
        CustomFixed,
        CustomVariable
    };

    private DataType type;

    public DataType getType() {
        return type;
    }

    protected void setType(DataType type) {
        this.type = type;
    }
}
