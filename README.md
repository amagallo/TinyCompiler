# Tiny Compiler & P-Code Virtual Machine

This project implements a full **compiler toolchain** for **Tiny**, a procedural programming language, together with a custom **stack-based virtual machine (P-code machine)** for program execution. The project demonstrates both compiler construction techniques and low-level systems programming for stack-based virtual machines.

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

- **Virtual Machine (P-Code):**
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

- **Documentation**
  - `/doc/Memoria Parte 1.pdf` – AST nodes & compiler phase specification (report from 1st submission)
  - `/doc/Memoria Parte 2.pdf` – Lexical & EBNF grammar specification (report from 2nd submission)
  - `/doc/readme.txt` – User manual for the compiler

- **Source Code**
  - `/src/alex` – Lexical definitions (token generation & lexical grammar)
  - `/src/asint` – Syntax & AST definitions (EBNF grammar & AST classes)
  - `/src/procesamientos` – Compiler phases (binding, typing, memory, code generation)
  - `/src/maquina` – Virtual machine core, P-code ISA, memory management
  - `/src/tiny` – Entry point & exception handling

- **Test Artifacts**
  - `/test/pass` – Valid Tiny code samples
  - `/test/fail` – Mutated variants of the valid samples

- **Examples**
  - `/examples/ejemplo.tiny` – Tiny program used as a demo for the project
  - `/examples/out.txt` – Object code generated from the previous Tiny file on the P-code machine

## 📖 References

Dechev D., Pirkelbauer P., Stroustrup B. (2006). *Lock-free Dynamically Resizable Arrays*. Springer.

Warren H. (2012). *Hacker’s Delight*. Addison-Wesley Professional.

## 👥 Authors

- Félix Rodolfo Díaz Lorente
- Álvaro Magalló Paz
- Alejandro del Río Caballero
