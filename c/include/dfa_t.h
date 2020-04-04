/*
 * DFA Reference Implementation.
 *
 * Copyright (C) 2020 Alexander Scheel <alexander.m.scheel@gmail.com>
 *
 * See LICENSE for more details.
 */

#include <stdint.h>

#pragma once

/* The DFA specification defines the following standard alphabets. */
typedef enum dfa_alphabet {
    dfa_ascii    = 0x01,
    dfa_utf8     = 0x02,
    dfa_utf16    = 0x03,
    dfa_utf32    = 0x04,
    dfa_uint8    = 0x05,
    dfa_uint16   = 0x06,
    dfa_uint32   = 0x07,
    dfa_uint64   = 0x08,
    dfa_fixed    = 0x09,
    dfa_variable = 0x0A
} dfa_alphabet;

/* A bounded range alphabet spec. */
typedef struct {
    uint64_t min_value;
    uint64_t max_value;
} dfa_alphabet_bounded_t;

/* A custom, fixed-size alphabet spec. */
typedef struct {
    uint64_t letter_width;
    uint8_t **letters;
} dfa_alphabet_custom_fixed_t;

/* A custom, variable-size alphabet spec. */
typedef struct {
    uint64_t width;
    uint8_t *letter;
} dfa_alphabet_variable_letter_t;

typedef struct {
    dfa_alphabet_variable_letter_t *letters;
} dfa_alphabet_custom_variable_t;

/* The DFA specification defines the following standard state types. */
typedef enum dfa_state {
    dfa_unnamed = 0x01,
    dfa_named   = 0x02
} dfa_state;

typedef struct {
    uint64_t *lengths;
    uint8_t **names;
} dfa_state_named;

/* The DFA specification defines the following termination types. */
typedef enum dfa_termination {
    dfa_reject_states = 0x01,
    dfa_accept_states = 0x02
} dfa_termination;

/* The DFA specification defines the following state shortcuts. */
typedef enum dfa_unknown_transitions {
    dfa_reject = 0x01,
    dfa_accept = 0x02,
    dfa_goto   = 0x03
} dfa_unknown_transitions;

typedef struct {
    uint64_t letter;
    uint64_t state;
} dfa_transition;

typedef struct {
    uint64_t state;
    dfa_unknown_transitions unknown;
    union {
        uint64_t goto_state;
    } unknown_data;
    uint64_t num_transitions;
    dfa_transition *transitions;
} dfa_transition_function;

/* DFA data structure. */
typedef struct {
    /* Alphabet Specifier */
    enum dfa_alphabet alphabet_type;
    uint64_t num_letters;
    union {
        dfa_alphabet_bounded_t            bounded;
        dfa_alphabet_custom_fixed_t       fixed;
        dfa_alphabet_custom_variable_t    variable;
    } alphabet_data;

    /* State Specifier */
    enum dfa_state state_type;
    union {
        dfa_state_named named;
    } state_data;
    uint64_t start_state;

    /* Terminations */
    enum dfa_termination termination_type;
    uint64_t num_terminating_states;
    uint64_t *terminating_states;

    /* Transitions */
    uint64_t *transition_length;
    dfa_transition_function *funcs;
} dfa_t;
