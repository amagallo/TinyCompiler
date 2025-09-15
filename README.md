# Tiny Compiler & Virtual Machine

This project implements a full **compiler toolchain** for **Tiny**, a procedural programming language, together with a custom **stack-based virtual machine (P-code machine)** for program execution. It was developed as the final project for the *Language Processors* course.

## 🚀 Features

- **Compiler front-end:**
  - Formal grammar written in **Extended BNF (EBNF)**.
  - Abstract Syntax Tree (AST) modeled in **Java 17 records & interfaces**, with syntactic sugar for binding, typing, memory assignment, and labels.
  - Full compilation pipeline: **lexical analysis, parsing, binding, typing, memory assignment, instruction labeling, and code generation**.

- **Processing pipeline:**
  - Reflection-based **dispatcher** to dynamically invoke processing methods across AST nodes.
  - **Memoization** of reflective lookups for performance optimization.
  - **Lazy definition** of processing phases (missing handlers simply do nothing).

- **Code generation:**
  - Structured control flow: `if/then[/else]`, `while/do`, `seq`.
  - Type-safe handling of **basic types**: integers, reals (with automatic **casting** from integers), booleans, strings.
  - Structured types: arrays, records, pointers with null-checking.
  - Procedure activation and return with support for **by-value / by-reference parameters**.

- **Virtual Machine (P-code):**
  - Instruction Set Architecture (ISA) including:
    - Arithmetic & logical operators (`+`, `-`, `*`, `/`, `%`, `and`, `or`, `not`, comparisons).
    - Memory operators (`apila`, `desapila`, `mueve`, indirect load/store).
    - Control flow (`ira`, `irf`, `irind`, procedure calls with activation records).
    - I/O (`read`, `write`, `endl`).
  - **Two-level RAM** (inspired by a concurrent vector implementation).
  - **FAT-style heap manager** for dynamic allocation/deallocation.
  - **Activation records** with control registers and static link display.
  - UTF-8 support for string input/output.

- **Robust exception handling:**
  - Global uncaught exception handler.
  - In **debug** mode: preserves filtered stack traces.
  - In **normal** mode: user-friendly error messages.  

## 📂 Repository Structure

```/src/alex``` – Lexical definitions (lexical grammar & token generation)

```/src/asint``` – Syntax & AST definitions (BNF grammar & AST classes)

```/src/procesamientos``` – Compiler phases (binding, typing, memory, code generation)

```/src/maquina``` – VM core, P-code ISA, memory management

```/src/tiny``` – Entry point & exception handling

## 📖 References

Dechev D., Pirkelbauer P., Stroustrup B. (2006). *Lock-free Dynamically Resizable Arrays*. [https://doi.org/10.1007/11945529_11](https://doi.org/10.1007/11945529_11)

Warren H. (2012). *Hacker’s Delight*. Addison-Wesley Professional.

## 👥 Authors

- Félix Rodolfo Díaz Lorente
- Álvaro Magalló Paz
- Alejandro del Río Caballero
