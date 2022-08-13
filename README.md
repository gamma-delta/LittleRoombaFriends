# Little Roomba Friends

roobma,

## The Roomba VM

A roomba is equipped with:

- 16 punchcard slots containing 16 instructions each
- 4 registers, A, B, C, and D, plus a pseudo-register for the instruction pointer
- 4 ports for peripherals
- Automatic item vacuum and inventory

Roombas will automatically pick up any items they move over, and try to deposit all the items in their inventory
into blocks they bump into.

Roombas also face a given direction, measured in degrees. 0 degrees is *south* (not north!), and positive degrees
increase counter-clockwise.

### Execution

A roomba's instruction pointer (IP) starts at card #0, index #0. At each execution step, it executes the command pointed
to by the IP and increments the IP. Once it reaches the end of a card, it goes back to the top of that card.
(To switch cards, use the `CRD` opcode.)

### Registers

The roomba's four registers each can contain an integer value between -999 and 999. Each of the registers has a special
purpose:

- `A`ccumulator is the most "general-purpose" register, and is used for arithmetic calculations.
- `B`ackup is used as a second argument/destination for calculations and peripherals.
- `C`onditional is used for conditional execution.
- `D`evice is used as a primary argument for peripherals.

Attempting to store a value outside of -999 through 999 in a register will saturate at the bounds.

```
LDA 600
ADD 600
; A now contains 999, not 1200
```

### Opcodes

Each opcode takes an *argument* and an optional *conditional flag.* The argument can be a literal integer between -999
and 999, one of the four registers, or a label. The conditional flag can be `+`, `-`, or `=`.

An opcode marked with the `+` conditional flag will be skipped unless the `C` register is greater than 0. Similarly,
the `-` flag requires the `C` register to be less than 0, and the `=` flag requires it to be equal.

### Labels

Labels can come before opcodes. They're simply any string followed by a colon, and can be used as arguments.

When compiling a program, labels used as arguments are syntactic sugar for integers; they represent the line they're
written on. This can help with writing `JMP` instructions.

### Comments

Any characters following a `;` character are comments, and are ignored.

---

So, an example program that moves the roomba across a 16x16 block rectangle:

```
LDA 16
LOOP:MOV 240 ; Sweep out a 2x15 rectangle
ROT 90
MOV 16
ROT 90
MOV 240
SUB 1
+ROT -90
+MOV 16
+ROT -90
+JMP LOOP
MOV 16
ROT 90
MOV 256
ROT 90
```

## Opcode Reference

Here, the letter `X` will refer to the argument of the opcode.

Opcodes are notated as their 3-letter representation in code, a mnemonic to remember the opcode by,
and what it does.

### Arithmetic

- `ADD`: *Add* `X` to the value in `A`, and store the result in `A`.
- `SUB`: *Sub*tract `X` from the value in `A`, and store the result in `A`
- `MUL`: *Mul*tiply `X` and the value in `A`, and store the result in `A`.
- `DVM`: *Div Mod*. Divide the value in `A` by `X`. Put the quotient in `A` and the remainder in `B`.

### Jumping

- `JMP`: *Jump* to line `X` in the given card, so that line `X` will be the next line executed.
- `JBY`: *Jump By.* Offset the instruction pointer by the given amount. Positive numbers will jump
  forward, negative numbers will jump backwards, and 0 will enter an infinite loop. The IP will wrap
  at the boundaries.
- `CRD`: *Card.* Move the instruction pointer to instruction 0 of the given card index. (Note that the IP
  automatically wraps around at the end of any given card, so there's no need to end cards with `CRD <the  
  current card index>`.)
- `CRJ`: *Card Jump.* Move the instruction pointer to the given card index, but keep the current line index.
  If a `CRJ` instruction is executed on line 8, then the next instruction executed will be line 8 on whatever card
  it is.

### Register Manipulation

- `LDA`: *Load A.* Copy `X` into `A`.
- `RLB`, `RLC`, `RLD`: *Roll through `B`/`C`/`D`*. Copy `X` into `A`, then copy the old value of `A` into
  `B`, `C`, or `D` depending on the opcode. This can be used to implement both a "copy" operation and a 
  "swap" operation. `RLB A` will copy the value of `A` to `B`, and `RLB B` will exchange the values in `A`
  and `B`.

### Roomba Manipulation

- `MOV`: *Move* the roomba forward `X` pixels, where 1 pixel is 1/16th of a block. Execution hangs
  until the roomba has moved that many pixels forward or bumped into a wall.
- `ROT`: *Rotate* the roomba by `X` degrees. `ROT 90` makes the roomba face orthogonally right, and
  `ROT -90` makes the roomba face orthogonally left.
- `PHL`: *Peripheral.* Interact with the peripheral in slot `X` (0-indexed). Peripherals all do different things, but
  they usually do some kind of operation with the `D` and/or `A` registers.
- `SLP`: *Sleep* for the given number of ticks, where 1 tick is 1/20 of a second. Execution hangs for that long, and
  the roomba will pick up at the next argument once it is through.

