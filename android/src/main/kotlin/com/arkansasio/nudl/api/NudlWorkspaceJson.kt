package com.arkansasio.nudl.api

import com.google.gson.Gson
import com.google.gson.JsonParseException

object NudlWorkspaceJson {
    private val gson = Gson()

    fun encode(workspace: NudlWorkspace): String {
        require(workspace.id.isNotBlank()) { "Workspace ID cannot be blank" }
        require(workspace.name.isNotBlank()) { "Workspace name cannot be blank" }
        require(workspace.files.map { it.name }.distinct().size == workspace.files.size) {
            "Workspace file names must be unique"
        }
        return gson.toJson(workspace)
    }

    @Throws(JsonParseException::class, IllegalArgumentException::class)
    fun decode(json: String): NudlWorkspace {
        require(json.isNotBlank()) { "Workspace JSON cannot be blank" }
        val workspace = gson.fromJson(json, NudlWorkspace::class.java)
            ?: throw IllegalArgumentException("Workspace JSON did not contain an object")
        require(workspace.id.isNotBlank()) { "Workspace ID cannot be blank" }
        require(workspace.name.isNotBlank()) { "Workspace name cannot be blank" }
        require(workspace.files.map { it.name }.distinct().size == workspace.files.size) {
            "Workspace file names must be unique"
        }
        require(workspace.files.all { it.name.isNotBlank() }) { "Workspace file names cannot be blank" }
        return workspace
    }
}
