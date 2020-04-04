package com.cipherboy.fa.dfa;

public class CustomFixedAlphabet extends AlphabetData {
    private long letterWidth;
    byte[][] letters;

    public CustomFixedAlphabet() {
        setType(DataType.CustomFixed);
    }

    public CustomFixedAlphabet(long letterWidth) {
        this();

        setWidth(letterWidth);
    }

    public CustomFixedAlphabet(long letterWidth, byte[][] letters) {
        this(letterWidth);

        setLetters(letters);
    }

    public long getWidth() {
        return letterWidth;
    }

    public void setWidth(long letterWidth) {
        this.letterWidth = letterWidth;
    }

    public byte[][] getLetters() {
        return letters;
    }

    public byte[] getLetter(long id) {
        assert(id < letters.length);
        return letters[(int) id];
    }

    public void setLetters(byte[][] letters) {
        this.letters = letters;
    }
}
