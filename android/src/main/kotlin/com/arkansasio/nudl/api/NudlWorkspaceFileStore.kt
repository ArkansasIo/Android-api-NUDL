package com.arkansasio.nudl.api

/** In-memory implementation for the first Android integration release. */
class NudlWorkspaceFileStore : NudlWorkspaceStore {
    private val workspaces = mutableMapOf<String, NudlWorkspace>()
    override suspend fun load(workspaceId: String): NudlWorkspace? = workspaces[workspaceId]
    override suspend fun save(workspace: NudlWorkspace) { workspaces[workspace.id] = workspace }
    override suspend fun delete(workspaceId: String) { workspaces.remove(workspaceId) }
}
