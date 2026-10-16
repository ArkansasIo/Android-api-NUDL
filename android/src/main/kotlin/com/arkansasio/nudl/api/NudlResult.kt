package com.arkansasio.nudl.api

data class GeneratedSource(
    val code: String,
    val target: String,
    val filename: String,
    val syntaxLanguage: String,
)

data class ExecutionResult(
    val output: List<String> = emptyList(),
    val returnValue: Any? = null,
    val executionTimeMs: Double = 0.0,
    val stepCount: Int = 0,
    val error: String? = null,
    val variables: Map<String, Any?> = emptyMap(),
)
