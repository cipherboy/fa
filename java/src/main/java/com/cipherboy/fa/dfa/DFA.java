package com.cipherboy.fa.dfa;

public class DFA {
    private AlphabetType alphabetType;
    private long numLetters;
    private AlphabetData alphabetData;

    private StateType stateType;
    private long numStates;
    private StateData stateData;
    private long startState;

    private TerminationType terminationType;
    private long[] terminatingStates;

    private TransitionFunction[] transitionFunctions;
}
