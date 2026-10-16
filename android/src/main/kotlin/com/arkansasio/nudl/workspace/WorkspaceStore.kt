package com.arkansasio.nudl.workspace

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class WorkspaceFile(
    val name: String,
    val content: String,
    val isBlueprint: Boolean = name.endsWith(".nblueprint"),
    val readOnly: Boolean = false,
)

class WorkspaceStore(context: Context) {
    private val prefs = context.getSharedPreferences("nudl_workspace", Context.MODE_PRIVATE)
    private val filesKey = "files"

    fun load(): MutableList<WorkspaceFile> {
        val raw = prefs.getString(filesKey, null) ?: return defaultFiles().toMutableList()
        return try {
            val array = JSONArray(raw)
            MutableList(array.length()) { i ->
                val item = array.getJSONObject(i)
                WorkspaceFile(
                    name = item.optString("name", "main.nudl"),
                    content = item.optString("content", ""),
                    isBlueprint = item.optBoolean("isBlueprint", item.optString("name").endsWith(".nblueprint")),
                    readOnly = item.optBoolean("readOnly", false),
                )
            }.ifEmpty { defaultFiles().toMutableList() }
        } catch (_: Exception) {
            defaultFiles().toMutableList()
        }
    }

    fun save(files: List<WorkspaceFile>) {
        val array = JSONArray()
        files.forEach { file ->
            array.put(JSONObject().apply {
                put("name", file.name)
                put("content", file.content)
                put("isBlueprint", file.isBlueprint)
                put("readOnly", file.readOnly)
            })
        }
        prefs.edit().putString(filesKey, array.toString()).apply()
    }

    fun reset() {
        save(defaultFiles())
    }

    private fun defaultFiles(): List<WorkspaceFile> = listOf(
        WorkspaceFile(
            "main.nudl",
            "fn main() -> i64 {\n    print(\"Hello from Noodle IDE\");\n    return 0;\n}"
        ),
        WorkspaceFile(
            "math.nudl",
            "fn add(a: i64, b: i64) -> i64 {\n    return a + b;\n}\n"
        ),
        WorkspaceFile("Graph.nblueprint", "// Visual Blueprint Graph: Graph.nblueprint", isBlueprint = true),
        WorkspaceFile("README.md", "# Noodle IDE\n\nNUDL 0.3 Android workspace.", readOnly = true),
    )
}
