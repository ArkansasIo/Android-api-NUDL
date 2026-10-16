package com.arkansasio.nudl

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arkansasio.nudl.api.NudlDiagnostic
import com.arkansasio.nudl.api.NudlRepository
import com.arkansasio.nudl.api.NudlServiceFactory
import com.arkansasio.nudl.api.NudlViewModel
import com.arkansasio.nudl.workspace.WorkspaceFile
import com.arkansasio.nudl.workspace.WorkspaceStore
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

private const val PRODUCT = "Noodle IDE"
private const val VERSION = "1.5.0-pre-alpha"
private const val BUILD_ID = "NUDL-NOODLE-1.5.0-PRE-ALPHA-TEST1"
private const val DEVELOPER = "Stephen Jr"

private val Navy = Color(0xFF07111C)
private val Panel = Color(0xFF091624)
private val Panel2 = Color(0xFF0B1827)
private val Editor = Color(0xFF06101A)
private val Border = Color(0xFF20334A)
private val Cyan = Color(0xFF6EE7FF)
private val TextPrimary = Color(0xFFE7F4FC)
private val TextMuted = Color(0xFF7F9CAE)
private val Green = Color(0xFF55D98A)
private val Amber = Color(0xFFF1C75B)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme { Surface(Modifier.fillMaxSize(), color = Navy) { NoodleBootThenApp() } }
        }
    }
}

@Composable
private fun NoodleBootThenApp() {
    var booting by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(650); booting = false }
    if (booting) BootScreen() else {
        val repository = remember { NudlRepository(NudlServiceFactory.create()) }
        val vm: NudlViewModel = viewModel(factory = NudlViewModelFactory(repository))
        NoodleWorkspace(vm)
    }
}

@Composable
private fun BootScreen() {
    Box(Modifier.fillMaxSize().background(Navy), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("N", color = Cyan, fontSize = 58.sp, fontWeight = FontWeight.Black)
            Text(PRODUCT, color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("ANDROID COMMAND WORKSPACE", color = TextMuted, fontSize = 11.sp, letterSpacing = 1.8.sp)
            Spacer(Modifier.height(18.dp))
            Text("$VERSION  •  $BUILD_ID", color = TextMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun NoodleWorkspace(viewModel: NudlViewModel) {
    val context = LocalContext.current
    val store = remember { WorkspaceStore(context) }
    var files by remember { mutableStateOf(store.load()) }
    var activeIndex by remember { mutableStateOf(0) }
    var sidebarOpen by remember { mutableStateOf(true) }
    var inspectorOpen by remember { mutableStateOf(true) }
    var mode by remember { mutableStateOf(if (files.firstOrNull()?.isBlueprint == true) "BLUEPRINT" else "TEXT") }
    var target by remember { mutableStateOf("C++") }
    var bottomTab by remember { mutableStateOf("TERMINAL") }
    var terminal by remember { mutableStateOf(listOf("Noodle IDE 0.3 CLI Terminal", "Type 'help' for commands.", "$ _")) }
    var newFileDialog by remember { mutableStateOf(false) }
    var deleteIndex by remember { mutableStateOf<Int?>(null) }
    var newFileName by remember { mutableStateOf("") }

    val state by viewModel.state.collectAsState()
    val activeFile = files.getOrNull(activeIndex) ?: WorkspaceFile("main.nudl", "")

    fun openFile(index: Int) {
        if (index !in files.indices) return
        activeIndex = index
        val file = files[index]
        mode = if (file.isBlueprint || file.name.endsWith(".nblueprint")) "BLUEPRINT" else "TEXT"
        viewModel.openFile(file.name, file.content)
        bottomTab = "OUTPUT"
    }

    fun saveCurrent(source: String) {
        if (activeIndex !in files.indices || files[activeIndex].readOnly) return
        val updated = files.toMutableList()
        updated[activeIndex] = updated[activeIndex].copy(content = source)
        files = updated
        store.save(updated)
    }

    fun formatCurrent() {
        val formatted = state.source.lines().joinToString("\n") { it.trimEnd() }
        viewModel.setSource(formatted)
        saveCurrent(formatted)
        bottomTab = "OUTPUT"
    }

    fun addFile() {
        val name = newFileName.trim().replace('\\', '/')
        if (name.isBlank() || name.startsWith("/") || name.split('/').any { it.isBlank() || it == "." || it == ".." }) return
        if (files.any { it.name.equals(name, ignoreCase = true) }) return
        val blueprint = name.endsWith(".nblueprint")
        val file = WorkspaceFile(name, if (blueprint) "// Visual Blueprint Graph: $name" else "fn main() -> i64 {\n    print(\"Hello from $name\");\n    return 0;\n}", blueprint)
        val updated = files + file
        files = updated
        store.save(updated)
        newFileDialog = false
        newFileName = ""
        openFile(updated.lastIndex)
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val project = JSONObject().apply {
                put("format", "NUDL-IDE-workspace")
                put("version", 1)
                put("files", JSONArray().apply {
                    files.forEach { f -> put(JSONObject().apply { put("name", f.name); put("content", f.content); put("isBlueprint", f.isBlueprint); put("readOnly", f.readOnly) }) }
                })
            }
            context.contentResolver.openOutputStream(uri)?.use { it.write(project.toString(2).toByteArray()) }
            terminal = listOf("[Workspace] Exported ${files.size} file(s).", "$ _")
            bottomTab = "OUTPUT"
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                val root = JSONObject(raw)
                require(root.optString("format") == "NUDL-IDE-workspace")
                val array = root.getJSONArray("files")
                val imported = MutableList(array.length()) { i ->
                    val f = array.getJSONObject(i)
                    WorkspaceFile(f.getString("name"), f.optString("content"), f.optBoolean("isBlueprint", false), f.optBoolean("readOnly", false))
                }
                if (imported.isNotEmpty()) {
                    files = imported
                    store.save(imported)
                    openFile(0)
                    terminal = listOf("[Workspace] Imported ${imported.size} file(s).", "$ _")
                    bottomTab = "OUTPUT"
                }
            } catch (_: Exception) {
                terminal = listOf("[Workspace Import Error] Invalid NUDL workspace.", "$ _")
                bottomTab = "PROBLEMS"
            }
        }
    }

    Column(Modifier.fillMaxSize().background(Navy)) {
        TopBar(sidebarOpen, { sidebarOpen = !sidebarOpen }, inspectorOpen, { inspectorOpen = !inspectorOpen }, onExport = { exportLauncher.launch("nudl-workspace.json") }, onImport = { importLauncher.launch(arrayOf("application/json", "text/json")) })
        ToolStrip(mode, { mode = it }, target, { target = it }, viewModel::analyze, viewModel::run, { viewModel.generate(target.lowercase()) }, ::formatCurrent, onNew = { newFileDialog = true })
        Row(Modifier.weight(1f).fillMaxWidth()) {
            if (sidebarOpen) SidebarPanel(files, activeIndex, ::openFile, { newFileDialog = true }, { deleteIndex = it }, mode, { mode = it }, ::formatCurrent, { bottomTab = it })
            EditorPanel(state.source, state.filename, { source -> viewModel.setSource(source); saveCurrent(source) }, state.running, mode)
            if (inspectorOpen) InspectorPanel(state.diagnostics, state.generatedSource != null, target)
        }
        BottomDock(bottomTab, { bottomTab = it }, state, terminal, onCommand = { command ->
            val cmd = command.trim()
            if (cmd.isEmpty()) return@BottomDock
            when {
                cmd.equals("clear", true) -> terminal = listOf("Noodle terminal cleared.", "$ _")
                cmd.equals("help", true) -> terminal = listOf("help  run  check  compile [c++]  compile [typescript]  compile [python]  ast  tokens  clear", "$ _")
                cmd.equals("run", true) -> { viewModel.run(); bottomTab = "CONSOLE"; terminal = terminal.dropLast(1) + listOf("$ run ${state.filename}", "Execution requested.", "$ _") }
                cmd.equals("check", true) -> { viewModel.analyze(); bottomTab = "PROBLEMS"; terminal = terminal.dropLast(1) + listOf("$ check ${state.filename}", "Diagnostics requested.", "$ _") }
                cmd.startsWith("compile", true) -> { val t = cmd.substringAfter(' ', "typescript").trim(); target = if (t.contains("cpp", true)) "C++" else if (t.contains("python", true)) "Python" else "TypeScript"; viewModel.generate(target.lowercase()); bottomTab = "TARGET"; terminal = terminal.dropLast(1) + listOf("$ nudlc ${state.filename} --target ${target.lowercase()}", "Code generation requested.", "$ _") }
                cmd.equals("ast", true) -> { bottomTab = "AST"; terminal = terminal.dropLast(1) + listOf("$ ast ${state.filename}", "AST inspector opened.", "$ _") }
                cmd.equals("tokens", true) -> { bottomTab = "TOKENS"; terminal = terminal.dropLast(1) + listOf("$ tokens ${state.filename}", "Token inspector opened.", "$ _") }
                else -> terminal = terminal.dropLast(1) + listOf("$ $cmd", "Unknown command. Type 'help'.", "$ _")
            }
        })
        StatusBar(state.running, state.diagnostics.size, target, files.size)
    }

    if (newFileDialog) {
        AlertDialog(onDismissRequest = { newFileDialog = false }, title = { Text("Create New File") }, text = { OutlinedTextField(value = newFileName, onValueChange = { newFileName = it }, label = { Text("Path, e.g. src/app.nudl") }) }, confirmButton = { TextButton(onClick = ::addFile) { Text("CREATE") } }, dismissButton = { TextButton(onClick = { newFileDialog = false }) { Text("CANCEL") } })
    }
    deleteIndex?.let { idx ->
        AlertDialog(onDismissRequest = { deleteIndex = null }, title = { Text("Delete File") }, text = { Text("Delete ${files.getOrNull(idx)?.name ?: "file"}?") }, confirmButton = { TextButton(onClick = { if (files.size > 1 && idx in files.indices) { val updated = files.toMutableList(); updated.removeAt(idx); files = updated; store.save(updated); openFile(minOf(idx, updated.lastIndex)) }; deleteIndex = null }) { Text("DELETE") } }, dismissButton = { TextButton(onClick = { deleteIndex = null }) { Text("CANCEL") } })
    }
}

@Composable
private fun TopBar(sidebar: Boolean, toggleSidebar: () -> Unit, inspector: Boolean, toggleInspector: () -> Unit, onExport: () -> Unit, onImport: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(52.dp).background(Panel).border(1.dp, Border).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("N", color = Cyan, fontSize = 25.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.width(9.dp)); Text(PRODUCT, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(" / WORKSPACE", color = TextMuted, fontSize = 11.sp)
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onImport) { Text("IMPORT", color = TextMuted, fontSize = 10.sp) }
        TextButton(onClick = onExport) { Text("EXPORT", color = Cyan, fontSize = 10.sp) }
        TextButton(onClick = toggleSidebar) { Text(if (sidebar) "HIDE TREE" else "TREE", color = Cyan, fontSize = 10.sp) }
        TextButton(onClick = toggleInspector) { Text(if (inspector) "HIDE INSPECTOR" else "INSPECTOR", color = Cyan, fontSize = 10.sp) }
    }
}

@Composable
private fun ToolStrip(mode: String, onMode: (String) -> Unit, target: String, onTarget: (String) -> Unit, analyze: () -> Unit, run: () -> Unit, generate: () -> Unit, format: () -> Unit, onNew: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(48.dp).background(Panel2).border(1.dp, Border).horizontalScroll(rememberScrollState()).padding(horizontal = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        ToolButton(if (mode == "TEXT") "TEXT" else "BLUEPRINT") { onMode(if (mode == "TEXT") "BLUEPRINT" else "TEXT") }
        ToolButton("NEW", onNew); ToolButton("ANALYZE", analyze); ToolButton("RUN", run); ToolButton("FORMAT", format); ToolButton("GENERATE", generate)
        Spacer(Modifier.width(5.dp)); Text("TARGET", color = TextMuted, fontSize = 9.sp)
        listOf("C++", "TypeScript", "Python").forEach { item -> TextButton(onClick = { onTarget(item) }) { Text(item, color = if (target == item) Cyan else TextMuted, fontSize = 10.sp) } }
    }
}

@Composable private fun ToolButton(label: String, onClick: () -> Unit) { Button(onClick = onClick, shape = RoundedCornerShape(4.dp), modifier = Modifier.height(33.dp)) { Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold) } }

@Composable
private fun SidebarPanel(files: List<WorkspaceFile>, selected: Int, onSelect: (Int) -> Unit, onNew: () -> Unit, onDelete: (Int) -> Unit, mode: String, onMode: (String) -> Unit, onFormat: () -> Unit, onTab: (String) -> Unit) {
    Column(Modifier.width(225.dp).fillMaxHeight().background(Panel).border(1.dp, Border).verticalScroll(rememberScrollState()).padding(7.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("SOLUTION EXPLORER", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); TextButton(onClick = onNew) { Text("+", color = Cyan, fontSize = 17.sp) } }
        Text("SOURCE & BLUEPRINTS", color = Color(0xFF60778E), fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp, 5.dp))
        files.forEachIndexed { index, file ->
            val indent = file.name.count { it == '/' } * 12
            TextButton(onClick = { onSelect(index) }, modifier = Modifier.fillMaxWidth().padding(start = indent.dp)) {
                val icon = if (file.isBlueprint) "◇" else if (file.name.contains("math")) "ƒ" else if (file.name.endsWith(".md")) "□" else "●"
                Text("$icon  ${file.name.substringAfterLast('/')}", color = if (index == selected) Cyan else TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        }
        Text("WORKSPACE VIEWS", color = Color(0xFF60778E), fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp, 10.dp, 8.dp, 5.dp))
        ViewButton("⌘  Text Code Editor", mode == "TEXT") { onMode("TEXT") }
        ViewButton("◇  Visual Blueprint Graph", mode == "BLUEPRINT") { onMode("BLUEPRINT") }
        Text("LANGUAGE TOOLS", color = Color(0xFF60778E), fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp, 10.dp, 8.dp, 5.dp))
        ViewButton("✦  Format Source") { onFormat() }
        ViewButton("▦  Token Inspector") { onTab("TOKENS") }
        ViewButton("▣  AST Inspector") { onTab("AST") }
        ViewButton(">_  CLI Terminal") { onTab("TERMINAL") }
        Spacer(Modifier.weight(1f))
        Text("NUDL 0.3", color = Cyan, fontFamily = FontFamily.Monospace, fontSize = 9.sp, modifier = Modifier.padding(8.dp))
        Text("Backend Target   Active", color = TextMuted, fontSize = 9.sp, modifier = Modifier.padding(8.dp))
    }
}

@Composable private fun ViewButton(label: String, selected: Boolean = false, onClick: () -> Unit) { TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(label, color = if (selected) Cyan else TextMuted, fontSize = 10.sp) } }

@Composable
private fun EditorPanel(source: String, filename: String, onSourceChange: (String) -> Unit, running: Boolean, mode: String) {
    Column(Modifier.weight(1f).fillMaxHeight().background(Editor).padding(7.dp)) {
        Row(Modifier.fillMaxWidth().height(36.dp).background(Panel2).border(1.dp, Border).padding(horizontal = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (mode == "TEXT") filename else "Graph.nblueprint", color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f)); Text(if (running) "● RUNNING" else "● READY", color = if (running) Amber else Green, fontSize = 9.sp)
        }
        Spacer(Modifier.height(5.dp))
        if (mode == "TEXT") {
            OutlinedTextField(value = source, onValueChange = onSourceChange, modifier = Modifier.fillMaxSize(), textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontFamily = FontFamily.Monospace), label = { Text("Noodle source", color = TextMuted) })
        } else BlueprintSurface()
    }
}

@Composable
private fun BlueprintSurface() {
    Box(Modifier.fillMaxSize().background(Color(0xFF081722)).border(1.dp, Border), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("VISUAL BLUEPRINT GRAPH", color = Cyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("Nodes • connections • compile to Noodle", color = TextMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 6.dp))
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { Node("INPUT"); Text("→", color = Cyan, fontSize = 19.sp); Node("FUNCTION"); Text("→", color = Cyan, fontSize = 19.sp); Node("OUTPUT") }
            Spacer(Modifier.height(18.dp))
            TextButton(onClick = {}) { Text("+ ADD NODE", color = Cyan, fontSize = 10.sp) }
        }
    }
}

@Composable private fun Node(label: String) { Box(Modifier.size(88.dp, 50.dp).background(Panel2, RoundedCornerShape(6.dp)).border(1.dp, Cyan, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) { Text(label, color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold) } }

@Composable
private fun InspectorPanel(diagnostics: List<NudlDiagnostic>, generated: Boolean, target: String) {
    val errors = diagnostics.count { it.severity.equals("error", true) }
    val warnings = diagnostics.count { it.severity.equals("warning", true) }
    Column(Modifier.width(215.dp).fillMaxHeight().background(Panel).border(1.dp, Border).verticalScroll(rememberScrollState()).padding(11.dp)) {
        Text("INSPECTOR", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp)); Info("LANGUAGE", "NUDL 0.3"); Info("ERRORS", "$errors"); Info("WARNINGS", "$warnings"); Info("CODEGEN", if (generated) "READY" else "IDLE"); Info("TARGET", target); Info("RUNTIME", "ANDROID")
        Text("PROPERTIES", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp))
        Text("Editor", color = TextPrimary, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)); Text("Syntax analysis: live", color = TextMuted, fontSize = 10.sp); Text("Workspace: persistent", color = TextMuted, fontSize = 10.sp)
    }
}

@Composable private fun Info(name: String, value: String) { Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text(name, color = TextMuted, fontSize = 9.sp, modifier = Modifier.weight(1f)); Text(value, color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold) } }

@Composable
private fun BottomDock(tab: String, select: (String) -> Unit, state: com.arkansasio.nudl.api.NudlEditorState, terminal: List<String>, onCommand: (String) -> Unit) {
    var command by remember { mutableStateOf("") }
    Column(Modifier.fillMaxWidth().height(175.dp).background(Color(0xFF07111F)).border(1.dp, Border)) {
        Row(Modifier.fillMaxWidth().height(35.dp).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
            listOf("TERMINAL", "OUTPUT", "PROBLEMS", "CONSOLE", "TARGET", "AST", "TOKENS").forEach { item -> TextButton(onClick = { select(item) }) { Text(item, color = if (tab == item) Cyan else TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold) } }
        }
        Column(Modifier.weight(1f).padding(10.dp).verticalScroll(rememberScrollState())) {
            when (tab) {
                "TERMINAL" -> terminal.forEach { Text(it, color = if (it.startsWith("$")) Cyan else TextMuted, fontFamily = FontFamily.Monospace, fontSize = 10.sp) }
                "OUTPUT" -> Text(if (state.running) "Process running…" else "Noodle Compiler 0.3 — workspace ready.\nTarget: Android API backend", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                "PROBLEMS" -> Problems(state.diagnostics)
                "CONSOLE" -> Text(if (state.running) "Process running…" else "Console ready. Run the current file to inspect execution.", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                "TARGET" -> Text(state.generatedSource ?: "No generated target yet. Use Generate.", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                "AST" -> AstView(state.source)
                "TOKENS" -> TokenView(state.source)
            }
        }
        if (tab == "TERMINAL") {
            Row(Modifier.fillMaxWidth().height(38.dp).padding(horizontal = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("$", color = Cyan, fontWeight = FontWeight.Bold); Spacer(Modifier.width(7.dp)); OutlinedTextField(value = command, onValueChange = { command = it }, modifier = Modifier.weight(1f), singleLine = true, textStyle = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontFamily = FontFamily.Monospace), placeholder = { Text("help | run | check | compile [target] | ast | tokens | clear", color = TextMuted, fontSize = 9.sp) }); TextButton(onClick = { if (command.isNotBlank()) { onCommand(command); command = "" } }) { Text("EXEC", color = Cyan, fontSize = 9.sp) }
            }
        }
    }
}

@Composable private fun Problems(items: List<NudlDiagnostic>) { if (items.isEmpty()) Text("✓  No syntax or semantic problems detected.", color = Green, fontSize = 10.sp) else items.forEach { Text("${it.severity.uppercase()}  ${it.message}", color = if (it.severity.equals("error", true)) Color(0xFFFF8080) else Amber, fontFamily = FontFamily.Monospace, fontSize = 10.sp, modifier = Modifier.padding(vertical = 2.dp)) } }
@Composable private fun AstView(source: String) { val lines = source.lines().filter { it.contains("fn ") || it.contains("struct ") || it.contains("return ") || it.contains("print(") }; Text(if (lines.isEmpty()) "Program → empty AST" else "Program\n" + lines.joinToString("\n") { "  └─ ${it.trim()}" }, color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 10.sp) }
@Composable private fun TokenView(source: String) { val tokens = Regex("[A-Za-z_][A-Za-z0-9_]*|[0-9]+|==|->|[{}()\\[\\],;:+*/=-]").findAll(source).map { it.value }.toList(); Text(if (tokens.isEmpty()) "No tokens" else tokens.mapIndexed { i, t -> "%04d  %s".format(i, t) }.joinToString("\n"), color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 10.sp) }

@Composable private fun StatusBar(running: Boolean, diagnostics: Int, target: String, fileCount: Int) { Row(Modifier.fillMaxWidth().height(26.dp).background(Editor).padding(horizontal = 9.dp), verticalAlignment = Alignment.CenterVertically) { Text(if (running) "RUNNING" else "READY", color = if (running) Amber else Green, fontSize = 8.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.width(12.dp)); Text("Problems $diagnostics", color = TextMuted, fontSize = 8.sp); Spacer(Modifier.width(12.dp)); Text("Files $fileCount", color = TextMuted, fontSize = 8.sp); Spacer(Modifier.weight(1f)); Text("NUDL • $target • Android • $DEVELOPER", color = TextMuted, fontSize = 8.sp) } }

private class NudlViewModelFactory(private val repository: NudlRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T { if (modelClass.isAssignableFrom(NudlViewModel::class.java)) { @Suppress("UNCHECKED_CAST") return NudlViewModel(repository) as T }; throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}") }
}
