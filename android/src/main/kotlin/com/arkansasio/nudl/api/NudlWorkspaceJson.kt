package com.arkansasio.nudl.api

import com.google.gson.Gson

object NudlWorkspaceJson {
    private val gson = Gson()
    fun encode(workspace: NudlWorkspace): String = gson.toJson(workspace)
    fun decode(json: String): NudlWorkspace = gson.fromJson(json, NudlWorkspace::class.java)
}
