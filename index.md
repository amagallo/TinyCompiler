# Tiny Compiler & P-Code Virtual Machine

This project implements a full **compiler toolchain** for **Tiny**, a procedural programming language, together with the **code generation tools** for a custom **stack-based P-code virtual machine**.  
The compiler covers all phases of translation, starting from a formal **EBNF grammar** and including lexical analysis, parsing, binding, typing, memory assignment, and final code generation.  
The virtual machine executes the generated P-code, supporting structured types, dynamic memory management, and robust exception handling.

## 🚀 Project Highlights
- Complete **compilation pipeline**: lexical analysis, parsing, binding, typing, memory assignment, and code generation.  
- **Abstract Syntax Tree** (AST) modeled in Java 17 with records and interfaces.  
- Reflection-based **dispatcher** with memoization for efficient processing.  
- Robust **code generation** for structured control flow, types, and procedures.  
- Custom **P-code virtual machine** with dynamic memory management and exception handling.

## 👥 Authors
- Félix Rodolfo Díaz Lorente
- Álvaro Magalló Paz
- Alejandro del Río Caballero

## 📝 License
This project is licensed under the **GNU General Public License v3.0 (GPLv3)**.  
See the [LICENSE](LICENSE) file for details.

## 📂 Repository
You can find the full source code, documentation, and examples here:  
👉 [GitHub Repository](https://github.com/amagallo/tiny-compiler)
