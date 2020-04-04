package com.cipherboy.fa.dfa;

public class GotoState extends UnknownData {
    private long state;

    public GotoState(long size) {
        setDataType(DataType.GotoState);
        setAdditionalDataSize(size);
    }

    public GotoState(long size, long state) {
        this(size);

        setState(state);
    }

    public long getState() {
        return state;
    }

    public void setState(long state) {
        this.state = state;
    }
}
