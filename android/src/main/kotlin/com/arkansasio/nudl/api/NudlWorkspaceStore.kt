package com.arkansasio.nudl.api

interface NudlWorkspaceStore {
    suspend fun load(workspaceId: String): NudlWorkspace?
    suspend fun save(workspace: NudlWorkspace)
    suspend fun delete(workspaceId: String)
}
