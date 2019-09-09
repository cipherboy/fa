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
  - It has a transition function mapping from the tuple (state, letter) to a
    state.
  - It has a single start state,
  - It has a set of accepting states.
 - While most machines are little endian, we use big-endian representations of
   integers to increase readability.
  - We use 1-byte, 2-byte, 4-byte, and 8-byte integer values; all are
    unsigned.


## File Format

We store a DFA as a binary file format. This has the limitation of making
parsing harder in some languages but has the benefit of improving access
patterns and avoids the memory restrictions a serialization format such as
JSON would place on us.


### Overview

Each DFA has the following fixed-sized five-byte header. After this header,
the rest of the file is of potentially variable-length. We assume that the
size of the entire DFA is known before hand, and thus need not be encoded
in the DFA itself.

     ----------- ------------- -----------------  ...
    | magic (3) | version (1) | alphabet-id (1) | ...
     ----------- ------------- -----------------  ...

After the header, there is an optional `alphabet-spec` section:

     ------------ -------------- ----------  ...
    | length (8) | num-elem (8) | data (v) | ...
     ------------ -------------- ----------  ...


### Magic Identifier (`magic`)

The first 3 bytes denote the type of the file; this is its magic value. We
define the magic value for a binary encoded DFA to be `{0x44, 0x46, 0x41}`,
which is DFA in ASCII.


### Version Number (`version`)

The next byte denotes the version of the file; this is version one of the
spec; the next byte is `{0x10}`.


### Alphabet

This section defines the alphabet-related fields and how to parse them.


#### Alphabet Identifier (`alphabet-id`)

The next byte denotes the alphabet used by the DFA. We define the following
values in this version of the spec:

 - `{0x01}` -- ASCII (max value `0x7F`)
 - `{0x02}` -- UTF-8
 - `{0x03}` -- UTF-16
 - `{0x04}` -- UTF-32
 - `{0x05}` -- 1-Byte Integer (`uint8_t`)
 - `{0x07}` -- 2-Byte Integer (`uint16_t`)
 - `{0x08}` -- 4-Byte Integer (`uint32_t`)
 - `{0x09}` -- 8-byte Integer (`uint64_t`)
 - `{0x0A}` -- Custom fixed-width data (`custom-fixed`)
 - `{0x0B}` -- Custom variable-width data (`custom-variable`)
 - Values 0x0C-0x41 are reserved for later versions of this spec.
 - Values 0x42 and above are reserved for implementation-defined values.

When values `0x01` to `0x04` are specified (ASCII to 8-Byte Integer), the next
section (`alphabet-spec`) is omitted. When value 0x05 or later is specified,
the `alphabet-spec` field has to be present. Values 0x05 to 0x09 allow either
an empty `alphabet-spec` (in which case, the full range of input integers of
the specified bit width are allowed), or a bounded range alphabet spec is
present, allowing the range of integers to be reduced. When values `0x0A` or
`0x0B` are specified, the `alphabet-spec` field has to be present and
non-empty.

Alphabets `0x01`, `0x02`, and `0x04` through `0x09` are easiest to parse: they
always have a fixed-sized identifier. Their identifier is the value of the
letter in the alphabet. While values `0x02` and `0x03` are harder to parse due
to their variable-width nature, they share the property that their identifier
is the value of the letter in the alphabet. Thus, they can be parsed directly.

Values `0x0A` and `0x0B` are harder to parse: they require the DFA library to
do the translation between letters in the alphabet and internal identifiers.
This is best suited for alphabets with few letters but whose letters are
sometimes lengthy strings. `0x0A` only allows letters of a fixed width,
whereas `0x0B` allows letters of a variable width. The length of the letter
identifier used in the remainder of the DFA depends on the size of the
alphabet; it is the smallest integer size (1, 2, 4, or 8 bytes, allowing a
value up to `2^{8*num_bytes} - 1` respectively) which fits the number of
elements in the custom field.

Note that, while a custom alphabet may be specified, it is sometimes better
to hide the alphabet parsing logic from the DFA, and store only integers. The
calling application can thus perform the translation between letters utilized
by the application and letters utilized by the DFA library. In this case, a
bounded range integer alphabet could be used to speed up parsing and reduce
DFA storage requirements.


#### Alphabet Specifier (`alphabet-spec`)

The next section defines the alphabet used by the DFA. It is present when the
alphabet identifier is `0x05` (`uint8_t`) or higher. The header for this
specification takes the following format:

     ------------ -------------- ----------
    | length (8) | num-elem (8) | data (v) |
     ------------ -------------- ----------

`data` is defined to be a variable-length field dependent on the choice of
alphabet to be stored.


##### Alphabet Specifier Length (`alphabet-spec-length`)

This section begins with an 8-byte integer value denoting the length of the
remainder of the `alphabet-spec` section. Since `num-elem` is a required
field, `length` must have value at least 8.


##### Alphabet Specifier Number of Elements (`alphabet-spec-num-elem`)

After the `length` field comes the 8-byte integer value denoting the number of
elements in the alphabet. Implementations use this value to determine the size
of the alphabet letter identifier field.


##### Empty Alphabet Specifier (`alphabet-spec-empty`)

When dealing with reserved spec values which don't require custom data but
which do require, give an empty alphabet specifier. This is done by specifying
a `length` field with value 8 and a `num-elem` of value 0.


##### Bounded Range Alphabet Specifier (`alphabet-spec-bounded`)

When an integer alphabet type (`0x05` to `0x09`) is specified, you can
optionally pass a range to accept numbers from; these are inclusive. These
are 8-byte integers specified in the required `data` field:

     --------------- ---------------
    | min-value (8) | max-value (8) |
     --------------- ---------------

In particular, this means that while 1-byte letters may be used internally
for storage, the numbers need only correspond to a 1-byte range. For example,
using the min-value of `0xFFFFFF00` to `0xFFFFFFFF` with an alphabet id of
`0x05` would let you internally store the range as 1 byte, even though the
values themselves are of 4 bytes. This provides DFA storage savings over using
the custom tag (`0x0A`, which would require listing all 4-byte values in the
range) or over using the 4-byte integer tag (`0x08`, optionally with another
bounded range specifier). This means that any integer values passed to the
library API must be 8-bytes.


##### Fixed-Width Elements (`alphabet-spec-custom-fixed`)

When the `custom-fixed` alphabet type is specified, the `num-elems` field must
be specified and non-zero. The `data` field takes


##### Variable-Width Elements (`alphabet-spec-custom-variable`)

### State

### Terminations

### Transition Index

### Transition Function

## Example DFAs

Below we give annotated examples of full DFAs.

### Binary String DFA

This DFA analyzes the given text and accepts it if and only if the entire
text is a binary number (e.g., the input string "10" (0x31, 0x30) would be
accepted but the input string " " (0x20) would not). We reject the empty
string (""), defining it not to be a valid binary number.

The entire DFA is 98 bytes:

    0x44 - 3 byte:  header ("DFA")
    0x46 -  ""                 ""
    0x41 -  ""                 ""
    0x01 - 1 byte: version (1)
    ----
    4 bytes - header

End of fixed-sized header. This is a DFA and version 1 of the spec.

    0x05 - 1 byte: alphabet-id (uint8_t)
    0x00 - 8 bytes: alphabet-spec-length (8 bytes)
    0x00 -  ""                                 ""
    0x00 -  ""                                 ""
    0x00 -  ""                                 ""
    0x00 -  ""                                 ""
    0x00 -  ""                                 ""
    0x00 -  ""                                 ""
    0x08 -  ""                                 ""
    0x00 - 8 bytes: alphabet-spec-num-elems (0)
    0x00 -  ""                              ""
    0x00 -  ""                              ""
    0x00 -  ""                              ""
    0x00 -  ""                              ""
    0x00 -  ""                              ""
    0x00 -  ""                              ""
    0x00 -  ""                              ""
    ----
    17 bytes - alphabet-spec

Note that we use alphabet `uint8_t` as we aren't worried about parsing UTF-8
correctly here and limit ourselves to parsing only a subset of it. We leave
out the optional bounded range specifier (and thus the `alphabet-spec-data`
field) to denote that we're using the default range for this alphabet of
`0x00` to `0xFF`.

    0x01 - 1 byte: state-id (unnamed)
    0x00 - 8 bytes: state-spec-length (8 bytes)
    0x00 -  ""                              ""
    0x00 -  ""                              ""
    0x00 -  ""                              ""
    0x00 -  ""                              ""
    0x00 -  ""                              ""
    0x00 -  ""                              ""
    0x08 -  ""                              ""
    0x00 - 8 bytes: state-spec-num-elems (2)
    0x00 -  ""                           ""
    0x00 -  ""                           ""
    0x00 -  ""                           ""
    0x00 -  ""                           ""
    0x00 -  ""                           ""
    0x00 -  ""                           ""
    0x02 -  ""                           ""
    0x00 - 1 byte: start-state (0)
    ----
    17 bytes - state-spec

Since we use state type `unnamed`, we can omit the optional `state-spec-data`
field. Our DFA will have two fields: a starting state (id: 0) and an accepting
state (id: 1). Due to a later parameter, we don't need an explicit failure
state because we fail on an unspecified transition.

Note that the `start-state` field is of length dependent on the value of
`state-spec-num-elems`. Since the latter's value is 2, each state will be
identified by a single byte.

    0x01 - 1 byte: accept-or-reject
    0x00 - 8 bytes: num-accept (1)
    0x00 -  ""                 ""
    0x00 -  ""                 ""
    0x00 -  ""                 ""
    0x00 -  ""                 ""
    0x00 -  ""                 ""
    0x00 -  ""                 ""
    0x01 -  ""                 ""
    0x01 - 1 byte: accepting state (0)
    ----
    10 bytes accept-spec

We specify that we're only listing accepting states (thus, the remainder are
rejecting states), and that there's a single accepting state: state with id 1.

    0x00 - 8 bytes: transitions-spec-length (10)
    0x00 -  ""                               ""
    0x00 -  ""                               ""
    0x00 -  ""                               ""
    0x00 -  ""                               ""
    0x00 -  ""                               ""
    0x00 -  ""                               ""
    0x10 -  ""                               ""
    0x00 - 8 bytes: state 0 transitions-spec-offset (0)
    0x00 -  ""                                      ""
    0x00 -  ""                                      ""
    0x00 -  ""                                      ""
    0x00 -  ""                                      ""
    0x00 -  ""                                      ""
    0x00 -  ""                                      ""
    0x00 -  ""                                      ""
    0x00 - 8 bytes: state 1 transitions-spec-offset (13)
    0x00 -  ""                                       ""
    0x00 -  ""                                       ""
    0x00 -  ""                                       ""
    0x00 -  ""                                       ""
    0x00 -  ""                                       ""
    0x00 -  ""                                       ""
    0x0D -  ""                                       ""
    ----
    24 bytes

The `transitions-spec` section provides pointers (relative to the end of the
section) to each of the state transitions. Note that this and possibly the
alphabet spec are the two portions most frequently accessed, and thus could
be loaded into memory (on resource constrained systems). In the event that
the entire transition table is too large to fit in memory, loading this index
allows the application to load only the transition table for only the current
state.

The length always a multiple of 8, because each state gets its own offset.

    0x00 - 8 bytes: state 0 num-transitions (2)
    0x00 -  ""                              ""
    0x00 -  ""                              ""
    0x00 -  ""                              ""
    0x00 -  ""                              ""
    0x00 -  ""                              ""
    0x00 -  ""                              ""
    0x02 -  ""                              ""
    0x01 - 1 byte: state 0: unknown-transition (reject)
    0x30 - 1 byte: state 0:0 transition-letter (0x30, "0")
    0x01 - 1 byte: state 0:0 transition-state (1)
    0x30 - 1 byte: state 0:1 transition-letter (0x31, "1")
    0x01 - 1 byte: state 0:1 transition-state (1)
    0x00 - 8 bytes: state 1 num-transitions (2)
    0x00 -  ""                              ""
    0x00 -  ""                              ""
    0x00 -  ""                              ""
    0x00 -  ""                              ""
    0x00 -  ""                              ""
    0x00 -  ""                              ""
    0x02 -  ""                              ""
    0x01 - 1 byte: state 1: unknown-transition (reject)
    0x30 - 1 byte: state 1:0 transition-letter (0x30, "0")
    0x01 - 1 byte: state 1:0 transition-state (1)
    0x30 - 1 byte: state 1:1 transition-letter (0x31, "1")
    0x01 - 1 byte: state 1:1 transition-state (1)
    ----
    26 bytes

Here we define the transition function of the DFA: "0" or "1" goes to the only
accepting state and implicitly, everything else causes the DFA to exit,
rejecting the input.

For this example, a relatively sparse DFA due to a number of implicitly
defined transitions, the use of large, 8-byte offsets bloats the DFA. The
headers alone are 72 bytes. However, at the expense of slightly large flash
storage, the resulting DFA doesn't require significant quantities of memory
to use. An optimized version capable of working on much large DFAs over the
same alphabet (and additional states) could get away with a couple of offsets
(to the `state-spec`, `accept-spec`, `transitions-spec`, and
`transition-list` sections) and a couple of correctly-sized variables, at the
cost of lots of random disk access.

Most implementations doing repeated lookups over the same, static DFA will
want to cache the transition table if possible, and use variable-sized data
structures better suited to the data at hand.
