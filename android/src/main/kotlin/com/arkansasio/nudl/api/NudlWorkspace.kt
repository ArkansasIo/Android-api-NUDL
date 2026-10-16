package com.arkansasio.nudl.api

data class NudlFile(val name: String, val source: String)

data class NudlWorkspace(
    val id: String,
    val name: String,
    val files: List<NudlFile> = emptyList(),
    val activeFile: String = "main.nudl",
)
