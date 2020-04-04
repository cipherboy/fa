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
 - While most machines that our DFAs are executed on are are little endian,
   we use big-endian representations of integers to increase human
   readability. in our examples.
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
 - `{0x06}` -- 2-Byte Integer (`uint16_t`)
 - `{0x07}` -- 4-Byte Integer (`uint32_t`)
 - `{0x08}` -- 8-byte Integer (`uint64_t`)
 - `{0x09}` -- Custom fixed-width data (`custom-fixed`)
 - `{0x0A}` -- Custom variable-width data (`custom-variable`)
 - Values 0x0B-0x4F are reserved for later versions of this spec.
 - Values 0x50 and above are reserved for implementation-defined values.

When values `0x01` to `0x04` are specified (ASCII to 8-Byte Integer), the next
section (`alphabet-spec`) is omitted. When value 0x05 or later is specified,
the `alphabet-spec` field has to be present. Values 0x05 to 0x09 allow either
an empty `alphabet-spec` (in which case, the full range of input integers of
the specified bit width are allowed), or a bounded range alphabet spec is
present, allowing the range of integers to be reduced. When values `0x0A` or
`0x0B` are specified, the `alphabet-spec` field has to be present and
non-empty.

Alphabets `0x01` and `0x04` through `0x09` are easiest to parse: they always
have a fixed-sized identifier. Their identifier is the value of the letter in
the alphabet. While values `0x02` and `0x03` are slightly harder to parse due
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
of the alphabet letter identifier field. Note that its exact purpose depends
on which algorithm specifier is present and does not necessarily imply that a
null alphabet was specified.


##### Empty Alphabet Specifier (`alphabet-spec-empty`)

Some usages of reserved alphabet spec values don't require custom data but
do require an Alphabet Specifier. In this case, give an empty alphabet
specifier. This is done by specifying a `length` field with value 8 and a
`num-elem` of value 0. This means that there is no `data` field.


##### Bounded Range Alphabet Specifier (`alphabet-spec-bounded`)

When an integer alphabet type (`0x05` to `0x09`) is specified, you can
optionally pass a range to accept numbers from; these are inclusive. These
are 8-byte integers specified in the required `data` field:

     --------------- ---------------
    | min-value (8) | max-value (8) |
     --------------- ---------------

In particular, this means that while 1-byte letters may be used internally
for storage, the numbers need only correspond to a 1-byte range. For example,
using the range of `0xFFFFFF00` to `0xFFFFFFFF` with an alphabet id of `0x05`
would let you internally store the alphabet as a 1 byte identifier, even though
the alphabet values themselves are of 4 bytes. This provides DFA storage savings
over using the custom tag (`0x0A`, which would require listing all 4-byte values
in the range) or over using the 4-byte integer tag (`0x08`, optionally with
another bounded range specifier). This means that any integer values passed to
the library API must be 8-bytes.

`alphabet-spec-num-elem` must be specified and correctly reflect the number of
elements (`max-value - min-value + 1`).


##### Fixed-Width Elements (`alphabet-spec-custom-fixed`)

When the `custom-fixed` alphabet type is specified, the `num-elems` field must
be specified and non-zero. The `data` field takes the form:

     ------------------ -------------------------  ...
    | letter-width (8) | letter 1 (letter-width) | ...
     ------------------ -------------------------  ...
     --------------------------------  ...
    | letter num-elem (letter-width) | ...
     --------------------------------  ...

The first field is `letter-width`, which is the width in bytes of each letter.
Then comes `num-elem` letters, each with width `letter-width` bytes. These are
ordered. That is, identifier 0 corresponds to the first letter slot,
identifier 1 the next, up to an identifier with value `num-elem` minus one.

This requires that `alphabet-spec-num-elem` must be specified and valid.

Each element is easily read for parsing: from the offset of the first letter
field (`letter 0`), it is easy to find the letter for the current identifier.
However, it is up to the application to handle mapping letters back onto
identifiers with an appropriate data structure.


##### Variable-Width Elements (`alphabet-spec-custom-variable`)

When the `custom-variable` alphabet type is specified, the `num-elems` field
must be specified and non-zero. The `data` field takes the form:

     -------------------- ---------------------------  ...
    | letter-1-width (8) | letter 1 (letter-1-width) | ...
     -------------------- ---------------------------  ...
     --------------------------- -----------------------------------------  ...
    | letter-num-elem-width (8) | letter num-elem (letter-num-elem-width) | ...
     --------------------------- -----------------------------------------  ...

Each letter width must be greater than zero.

In other words, each letter is a pair of `(size, bytes-of-letter)`, taking up
at least 9 bytes total. Parsing this data structure is much harder: because
each letter takes up a variable width, the application cannot jump simply to
the letter specified by the identifier. It is suggested that applications
build alternative in-memory data structures for suitably sized DFAs instead.


### State

This section defines the state-related fields and how to parse them.

States begin with value 0. They are encoded elsewhere as a variable number of
bytes (1, 2, 4, or 8), depending on the number of elements (defined below in
`state-spec-num-elem`).

States can be named with variable-width identifiers, allowing different
implementations to use the same names without sending additional data.


#### State Identifier (`state-id`)

The next byte denotes the state used by the DFA. We define the following
values in this version of the spec:

 - `{0x01}` -- unnamed states (numerical ids only)
 - `{0x02}` -- named states
 - Values 0x02-0x4F are reserved for later versions of this spec.
 - Values 0x50 and above are reserved for implementation defined values.

Including named states allows DFAs created on one machine to use the same
names for the states on a different machine.


#### State Specifier (`state-spec`)

The next section defines the states used by the DFA. It is always present. The
header for this specification takes the following format:

     ------------ -------------- ---------- -----------------
    | length (8) | num-elem (8) | data (v) | start-state (v) |
     ------------ -------------- ---------- -----------------

`data` is defined to be a variable-length field dependent on the choice of
state to be stored.


##### State Specifier Length (`state-spec-length`)

This section begins with an 8-byte integer value denoting the length of the
remainder of the `state-spec` section. Since `num-elem` and `start-state`
are both required fields, `length` must have value at least 8 plus the minimum
number of bytes to specify the starting state.


##### State Specifier Number of Elements (`state-spec-num-elem`)

After the `length` field comes the 8-byte integer value denoting the number
of states in the DFA. Implementations use this value to determine the size of
the state identifier field. Unlike the `alphabet-spec-num-elem` field, this
field always exactly identifies the number of states in the DFA.


##### State Specifier Empty (`state-spec-empty`)

Some usages of a state specifier don't require any custom data, even though
the `state-spec` section is mandatory. In this case, give an empty state
specifier, omitting the `data` field.


##### State Specifier Named (`state-spec-named`)

When the value of the `state-spec-length` exceeds 8 bytes, this implies data
is non-empty. We define a named state specifier as follows:

     ----------------------- ---------------------  ...
    | state-name-length (8) | state-name-data (v) | ...
     ----------------------- ---------------------  ...

Each state has the above structure repeated. This gives an `O(n)` lookup time
to access the name of any given state; it is suggested that implementations
optimize this when necessary.

#### Start State (`state-spec-start`)

After the `state-spec-data` field comes a variable width starting state
identifier. Length of this field depends on the number of states encoded
in the `state-spec-num-elem` field.


### Terminations

This section defines the set of terminating states in the DFA.


#### Termination Spec (`term-spec`)

The next byte determines whether the of terminating states is given as a list
of accepted states or a list of rejected states. This allows for encoding
whichever list of states is smaller. We define the following values in this
version of the spec:

 - `{0x01}` -- accept,
 - `{0x02}` -- reject,
 - Values 0x03-0x4F are reserved for later versions of this spec.
 - Values 0x50 and above are reserved for implementation-defined values.


#### Termination States (`term-states`)

The next section defines the set of termination states with the above status
(accept or reject). The header for this specification takes the following
format:

     ------------ ----------
    | length (8) | data (v) |
     ------------ ----------

`length` is the size of the data segment, in bytes, and `data` is a list of
state identifiers. Each data element is the width of a single state; this is
given above by the number of states as defined in the `state-spec-num-elem`
field. The minimum number of bits required to parse this field should be used
when encoding it.


### Transition Index

This section defines the index of transition functions.


#### Transition Index Specifier (`transition-index-spec`)

The next section defines the offsets (relative to the end of this section)
of the transition functions for each state. The header for this specification
takes the following format:

     ------------ --------------------       ---------------------------
    | length (8) | state-0-offset (8) | ... | state-num-elem-offset (8) |
     ------------ --------------------       ---------------------------

Note that each state must define a state transition function, even if it only
accepts or rejects all transitions out of it. This makes the total size of
this section `8 + 8*num-elem` bytes.


### Transition Function

This section defines the state transitions for a single state.


#### Transition Function Specifier (`transition-func-spec`)

The next section defines the transition function for a particular starting
state. The header for this specification takes the following format:

     ------------ -------------- -------------- -------------
    | length (8) | unknown (1+) | letter-1 (v) | state-1 (v) | ...
     ------------ -------------- -------------- -------------
     -------------- -------------
    | letter-n (v) | state-n (v) |
     -------------- -------------

`length` encodes the number of bytes in the remainder of this transition
function specifier. It must be at least 1, to encode what happens to an
unknown state. The number of defined transitions can be calculated by
computing:

                     length - sizeof(unknown)
    transitions = ------------------------------
                  sizeof(letter) + sizeof(state)

Where `sizeof(letter)` is the size of a letter identifier (defined above
based on `alphabet-id`), and `sizeof(state)` is the size of a state
identifier (defined above based on `state-spec-num-elem`). Determining
`sizeof(unknown)` is defined below.

Because this is a DFA, each `letter` must be unique within a transition
function specifier section.


#### Transition Function Unknown Transitions (`transition-func-spec-unknown`)

When the given letter is not found in the transition function, a default
must be provided. The following identifiers are defined:

 - `{0x01}` - reject all (1 byte)
 - `{0x02}` - accept all (1 byte)
 - `{0x03 ...}` - go to state (1 + `v` bytes)
 - Values 0x04-0x4F are reserved for future versions of this specification.
 - Values 0x50 and above are implementation defined.

In the case of accept all or reject all, this field is one byte. However,
in the case of value `0x03` in the first byte, the width of this field is
the size of a state identifier plus one, allowing the remaining bytes to
encode the desired state. This means that the first byte must be read before
the length of this field can be computed.


#### Transition Function Definition (`transition-func-spec-def`)

The last component of the transition function specification defines the
transition function relative to a particular current state:

     ------------ -----------
    | letter (v) | state (v) |
     ------------ -----------

The width of a letter is determined above by `alphabet-id` and the width of
the state identifier is determined above by `state-spec-num-elem`.


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
    <no alphabet-spec-data field>
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
    <no state-spec-data field>
    0x00 - 1 byte: start-state (0)
    ----
    17 bytes - state-spec

Since we use state type `unnamed`, we can omit the optional `state-spec-data`
field. Our DFA will have two fields: a starting state (id: 0) and an accepting
state (id: 1). Due to a later parameter, we don't need an explicit failure
state because we fail on an unspecified transition.

Note that the size of the `start-state` field is of length dependent on the
value of `state-spec-num-elems`. Since the latter's value is 2, each state
will be identified by a single byte.

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
