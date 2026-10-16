package com.arkansasio.nudl.api

object NudlTargets {
    const val INTERPRETER = "Interpreter"
    const val CPP = "C++"
    const val C = "C"
    const val RUST = "Rust"
    const val PYTHON = "Python"
    const val JAVASCRIPT = "JavaScript"
    const val TYPESCRIPT = "TypeScript"
    const val CSHARP = "C#"
    const val JAVA = "Java"
    const val SWIFT = "Swift"
    const val WASM = "WASM"

    val all = listOf(INTERPRETER, CPP, C, RUST, PYTHON, JAVASCRIPT, TYPESCRIPT, CSHARP, JAVA, SWIFT, WASM)
}
