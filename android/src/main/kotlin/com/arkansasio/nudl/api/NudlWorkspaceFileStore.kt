package com.arkansasio.nudl.api

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * File-backed workspace store using the application's private storage.
 *
 * Kept under the historical class name so existing callers can migrate
 * without changing construction sites; use FileNudlWorkspaceStore(context)
 * in new code when a more explicit name improves readability.
 */
class NudlWorkspaceFileStore(context: Context) : NudlWorkspaceStore {
    private val root = File(context.filesDir, "nudl-workspaces").apply {
        if (!exists() && !mkdirs()) {
            throw IllegalStateException("Unable to create NUDL workspace directory")
        }
    }

    override suspend fun load(workspaceId: String): NudlWorkspace? = withContext(Dispatchers.IO) {
        val file = fileFor(workspaceId)
        if (!file.isFile) return@withContext null
        runCatching { NudlWorkspaceJson.decode(file.readText(Charsets.UTF_8)) }.getOrNull()
    }

    override suspend fun save(workspace: NudlWorkspace) = withContext(Dispatchers.IO) {
        require(workspace.id.isNotBlank()) { "Workspace ID cannot be blank" }
        val file = fileFor(workspace.id)
        val temporary = File(root, "${file.name}.tmp")
        temporary.writeText(NudlWorkspaceJson.encode(workspace), Charsets.UTF_8)
        if (!temporary.renameTo(file)) {
            temporary.delete()
            throw IllegalStateException("Unable to commit workspace file")
        }
    }

    override suspend fun delete(workspaceId: String) = withContext(Dispatchers.IO) {
        val file = fileFor(workspaceId)
        if (file.exists() && !file.delete()) {
            throw IllegalStateException("Unable to delete workspace")
        }
    }

    private fun fileFor(workspaceId: String): File {
        require(workspaceId.isNotBlank()) { "Workspace ID cannot be blank" }
        val safeName = workspaceId.replace(Regex("[^A-Za-z0-9._-]"), "_")
        require(safeName != "." && safeName != "..") { "Invalid workspace ID" }
        return File(root, "$safeName.json")
    }
}
