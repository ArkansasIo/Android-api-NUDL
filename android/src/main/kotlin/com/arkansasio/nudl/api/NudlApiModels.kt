package com.arkansasio.nudl.api

data class NudlRequest(
    val source: String,
    val filename: String = "main.nudl",
    val target: String? = null,
)

data class NudlDiagnostic(
    val code: String? = null,
    val message: String,
    val line: Int? = null,
    val col: Int? = null,
    val severity: String? = null,
)

data class NudlResponse<T>(
    val ok: Boolean,
    val diagnostics: List<NudlDiagnostic> = emptyList(),
    val result: T? = null,
    val error: String? = null,
)

data class HealthResponse(
    val ok: Boolean,
    val service: String,
    val version: String,
)
