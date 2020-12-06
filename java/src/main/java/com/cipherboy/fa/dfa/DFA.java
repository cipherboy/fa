package com.cipherboy.fa.dfa;

/**
 * A DFA is a type of finite automata.
 */
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

    public DFA() {}

    public Letter addLetter() {
        numLetters += 1;
        return new Letter(this);
    }
}
