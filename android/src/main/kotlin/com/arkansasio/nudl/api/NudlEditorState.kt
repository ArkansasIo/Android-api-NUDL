package com.arkansasio.nudl.api

data class NudlEditorState(
    val filename: String = "main.nudl",
    val source: String = "",
    val diagnostics: List<NudlDiagnostic> = emptyList(),
    val running: Boolean = false,
    val generatedSource: String? = null,
    val generatedFilename: String? = null,
)
