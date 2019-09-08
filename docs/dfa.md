# Deterministic Finite Automata (DFA)

In this document, we describe a format for serializing a DFA.


## Overview of Goals

We seek to express a DFA in a format that easily serializes and places
relatively low constraints on the creation and use of large automata.
In particular, we wish to enable libraries which give the callers choices
about the quantity of the DFA that gets parsed. More concretely, our goals
are:

 1. To create a format that serializes a DFA,
 2. That allows for expression of arbitrary languages,
 3. That allows for arbitrary state names,
 4. That allows for a trade off of on DFA construction,
 5. That allows for lazy parsing of a DFA's state transitions,
 6. That allows for the DFA to be extended.

In 4, we wish to express a trade off on DFA construction that allows for
the DFA to be constructed live, while serializing to disk, in a manner that
allows memory usage to be reduced at the expense of additional disk reads
and writes.

In 5, we wish to be able to use a DFA (and, follow its transitions) without
necessarily resorting to reading the entire file into memory and parsing it
all. We wish to create a format that allows for reading a "header" (containing
information necessary for validating our transitions), and then load only
states and their transitions as necessary to parse the start string. This type
of access is best suited for a DFA stored on a random-access storage media
like a SSD.

In 6, we wish to enable the DFA to be expanded at a later date. That is, it
need not be complete when initially written to disk. This is a direct
consequence of item 4. Additionally, we impose the additional constraint that
extending an already-serialized DFA will not require storing the entire DFA
in memory at once, but may require incrementally rewriting the entire DFA.

Our resulting format has a limitation as a result of goals 3-6: two DFAs with
equal states, alphabets, and transitions may not have the same format on disk.
This is because they may have been constructed differently, resulting in
different ordering of states, or even in redundant data.


## Definitions

We assume the following in the rest of the document:

 - A Deterministic Finite Automata follows the standard definition, namely:
  - It has an alphabet set `A`,
  - It has a state set `S`,


## File Format

We store a DFA as a binary file format. This has the limitation of making
parsing harder in some languages but has the benefit of improving access
patterns and avoids the memory restrictions a serialization format such as
JSON would place on us.

