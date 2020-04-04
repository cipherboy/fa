package com.cipherboy.fa.dfa;

public abstract class UnknownData {
    public enum DataType {
        GotoState;
    }

    private DataType type;
    private long additionalDataSize;

    public DataType getDataType() {
        return type;
    }

    protected void setDataType(DataType type) {
        this.type = type;
    }

    public long getAdditionalDataSize() {
        return additionalDataSize;
    }

    protected void setAdditionalDataSize(long length) {
        this.additionalDataSize = length;
    }
}
