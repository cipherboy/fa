package com.cipherboy.fa.dfa;

public class TransitionFunction {
    private long from;
    private UnknownTransitions unknown;
    private long[] transitionLetters;
    private long[] transitionTos;

    public TransitionFunction(long from) {
        this.from = from;
    }

    public TransitionFunction(long from, UnknownTransitions unknown) {
        this(from);

        setUnknownTransitions(unknown);
    }

    public UnknownTransitions getUnknownTransitions() {
        return unknown;
    }

    public void setUnknownTransitions(UnknownTransitions unknown) {
        this.unknown = unknown;
    }
}
