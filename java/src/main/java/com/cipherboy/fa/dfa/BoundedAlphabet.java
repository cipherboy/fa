package com.cipherboy.fa.dfa;

public final class BoundedAlphabet extends AlphabetData {
    private long minValue;
    private long maxValue;

    public BoundedAlphabet() {
        setType(DataType.Bounded);
    }

    public BoundedAlphabet(long minValue, long maxValue) {
        this();

        setMaxValue(maxValue);
        setMinValue(minValue);
    }

    public long getMinValue() {
        return minValue;
    }

    public long getMaxValue() {
        return maxValue;
    }

    protected void setMinValue(long minValue) {
        this.minValue = minValue;
    }

    protected void setMaxValue(long maxValue) {
        this.maxValue = maxValue;
    }
}
