package com.arkansasio.nudl

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
import com.arkansasio.nudl.api.NudlEditorState
import com.arkansasio.nudl.api.NudlRepository
import com.arkansasio.nudl.api.NudlServiceFactory
import com.arkansasio.nudl.api.NudlViewModel
import com.arkansasio.nudl.workspace.WorkspaceFile
import com.arkansasio.nudl.workspace.WorkspaceStore
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

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

private const val PRODUCT = "Noodle IDE"
private const val VERSION = "1.5.0-pre-alpha"
private const val BUILD_ID = "NUDL-NOODLE-1.5.0-PRE-ALPHA-TEST1"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { Surface(Modifier.fillMaxSize(), color = Navy) { NoodleApp() } } }
    }
}

@Composable
private fun NoodleApp() {
    var boot by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(650); boot = false }
    if (boot) BootScreen() else {
        val repository = remember { NudlRepository(NudlServiceFactory.create()) }
        val vm: NudlViewModel = viewModel(factory = NudlVmFactory(repository))
        Workspace(vm)
    }
}

@Composable
private fun BootScreen() {
    Box(Modifier.fillMaxSize().background(Navy), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("N", color = Cyan, fontSize = 58.sp, fontWeight = FontWeight.Black)
            Text(PRODUCT, color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("ANDROID COMMAND WORKSPACE", color = TextMuted, fontSize = 11.sp)
            Spacer(Modifier.height(14.dp))
            Text("$VERSION • $BUILD_ID", color = TextMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun Workspace(vm: NudlViewModel) {
    val context = LocalContext.current
    val store = remember { WorkspaceStore(context) }
    var files by remember { mutableStateOf(store.load()) }
    var selected by remember { mutableStateOf(0) }
    var mode by remember { mutableStateOf("TEXT") }
    var target by remember { mutableStateOf("C++") }
    var tab by remember { mutableStateOf("TERMINAL") }
    var sidebar by remember { mutableStateOf(true) }
    var inspector by remember { mutableStateOf(true) }
    var newDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var terminal by remember { mutableStateOf(listOf("Noodle IDE 0.3 CLI Terminal", "Type 'help' for commands.", "$ _")) }
    val state by vm.state.collectAsState()

    fun open(index: Int) {
        if (index !in files.indices) return
        selected = index
        val f = files[index]
        mode = if (f.isBlueprint || f.name.endsWith(".nblueprint")) "BLUEPRINT" else "TEXT"
        vm.openFile(f.name, f.content)
        tab = "OUTPUT"
    }
    fun save(source: String) {
        if (selected !in files.indices || files[selected].readOnly) return
        val copy = files.toMutableList()
        copy[selected] = copy[selected].copy(content = source)
        files = copy
        store.save(copy)
    }
    fun createFile() {
        val name = newName.trim().replace('\\', '/')
        if (name.isBlank() || files.any { it.name.equals(name, true) }) return
        val bp = name.endsWith(".nblueprint")
        val content = if (bp) "// Noodle Blueprint: $name" else "fn main() -> i64 {\n    print(\"Hello from Noodle\");\n    return 0;\n}\n"
        val copy = (files + WorkspaceFile(name, content, bp)).toMutableList()
        files = copy
        store.save(copy)
        newDialog = false
        newName = ""
        open(copy.lastIndex)
    }

    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val root = JSONObject().apply {
                put("format", "NUDL-IDE-workspace")
                put("version", 1)
                put("files", JSONArray().apply {
                    files.forEach { f -> put(JSONObject().apply {
                        put("name", f.name); put("content", f.content)
                        put("isBlueprint", f.isBlueprint); put("readOnly", f.readOnly)
                    }) }
                })
            }
            context.contentResolver.openOutputStream(uri)?.use { it.write(root.toString(2).toByteArray()) }
            terminal = listOf("[Workspace] Exported ${files.size} file(s).", "$ _")
            tab = "OUTPUT"
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) try {
            val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
            val root = JSONObject(raw)
            require(root.optString("format") == "NUDL-IDE-workspace")
            val a = root.getJSONArray("files")
            val copy = MutableList(a.length()) { i ->
                val f = a.getJSONObject(i)
                WorkspaceFile(f.getString("name"), f.optString("content"), f.optBoolean("isBlueprint"), f.optBoolean("readOnly"))
            }.toMutableList()
            if (copy.isNotEmpty()) {
                files = copy; store.save(copy); open(0)
                terminal = listOf("[Workspace] Imported ${copy.size} file(s).", "$ _")
            }
        } catch (_: Exception) {
            terminal = listOf("[Workspace Import Error] Invalid workspace.", "$ _")
            tab = "PROBLEMS"
        }
    }

    Column(Modifier.fillMaxSize().background(Navy)) {
        Row(Modifier.fillMaxWidth().height(52.dp).background(Panel).border(1.dp, Border).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("N", color = Cyan, fontSize = 25.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(9.dp)); Text(PRODUCT, color = TextPrimary, fontWeight = FontWeight.Bold)
            Text(" / WORKSPACE", color = TextMuted, fontSize = 10.sp)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { import.launch(arrayOf("application/json", "text/json")) }) { Text("IMPORT", color = TextMuted, fontSize = 9.sp) }
            TextButton(onClick = { export.launch("nudl-workspace.json") }) { Text("EXPORT", color = Cyan, fontSize = 9.sp) }
            TextButton(onClick = { sidebar = !sidebar }) { Text(if (sidebar) "HIDE TREE" else "TREE", color = Cyan, fontSize = 9.sp) }
            TextButton(onClick = { inspector = !inspector }) { Text(if (inspector) "HIDE INSPECTOR" else "INSPECTOR", color = Cyan, fontSize = 9.sp) }
        }
        Row(Modifier.fillMaxWidth().height(46.dp).background(Panel2).border(1.dp, Border).horizontalScroll(rememberScrollState()).padding(horizontal = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            ActionButton(if (mode == "TEXT") "TEXT" else "BLUEPRINT") { mode = if (mode == "TEXT") "BLUEPRINT" else "TEXT" }
            ActionButton("NEW") { newDialog = true }
            ActionButton("ANALYZE") { vm.analyze(); tab = "PROBLEMS" }
            ActionButton("RUN") { vm.run(); tab = "CONSOLE" }
            ActionButton("FORMAT") { val s = state.source.lines().joinToString("\n") { it.trimEnd() }; vm.setSource(s); save(s); tab = "OUTPUT" }
            ActionButton("GENERATE") { vm.generate(target.lowercase()); tab = "TARGET" }
            Text("TARGET", color = TextMuted, fontSize = 9.sp)
            listOf("C++", "TypeScript", "Python").forEach { t -> TextButton(onClick = { target = t }) { Text(t, color = if (target == t) Cyan else TextMuted, fontSize = 9.sp) } }
        }
        Row(Modifier.weight(1f).fillMaxWidth()) {
            if (sidebar) FileTree(files, selected, ::open, { newDialog = true }, { tab = it })
            Editor(state, mode, Modifier.weight(1f)) { s -> vm.setSource(s); save(s) }
            if (inspector) Inspector(state, target)
        }
        BottomDock(tab, { tab = it }, state, terminal) { cmd ->
            when (cmd.trim().lowercase()) {
                "help" -> terminal = listOf("help  run  check  compile cpp|typescript|python  ast  tokens  clear", "$ _")
                "run" -> { vm.run(); tab = "CONSOLE"; terminal = listOf("$ run ${state.filename}", "Execution requested.", "$ _") }
                "check" -> { vm.analyze(); tab = "PROBLEMS"; terminal = listOf("$ check ${state.filename}", "Diagnostics requested.", "$ _") }
                "clear" -> terminal = listOf("Noodle terminal cleared.", "$ _")
                "ast" -> tab = "AST"
                "tokens" -> tab = "TOKENS"
                else -> if (cmd.trim().startsWith("compile", true)) { vm.generate(target.lowercase()); tab = "TARGET" } else terminal = listOf("$ ${cmd.trim()}", "Unknown command. Type 'help'.", "$ _")
            }
        }
        Row(Modifier.fillMaxWidth().height(26.dp).background(Editor).padding(horizontal = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (state.running) "RUNNING" else "READY", color = if (state.running) Amber else Green, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(12.dp)); Text("Problems ${state.diagnostics.size}", color = TextMuted, fontSize = 8.sp)
            Spacer(Modifier.width(12.dp)); Text("Files ${files.size}", color = TextMuted, fontSize = 8.sp)
            Spacer(Modifier.weight(1f)); Text("NUDL • $target • Android", color = TextMuted, fontSize = 8.sp)
        }
    }
    if (newDialog) AlertDialog(onDismissRequest = { newDialog = false }, title = { Text("Create New File") }, text = { OutlinedTextField(newName, { newName = it }, label = { Text("Path, e.g. src/app.nudl") }) }, confirmButton = { TextButton(onClick = ::createFile) { Text("CREATE") } }, dismissButton = { TextButton(onClick = { newDialog = false }) { Text("CANCEL") } })
}

@Composable
private fun ActionButton(label: String, action: () -> Unit) = Button(onClick = action, modifier = Modifier.height(32.dp), shape = RoundedCornerShape(4.dp)) { Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold) }

@Composable
private fun FileTree(files: List<WorkspaceFile>, selected: Int, open: (Int) -> Unit, newFile: () -> Unit, tab: (String) -> Unit) {
    Column(Modifier.width(220.dp).fillMaxHeight().background(Panel).border(1.dp, Border).verticalScroll(rememberScrollState()).padding(7.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("SOLUTION EXPLORER", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); TextButton(onClick = newFile) { Text("+", color = Cyan) } }
        Text("SOURCE & BLUEPRINTS", color = TextMuted, fontSize = 8.sp, modifier = Modifier.padding(8.dp))
        files.forEachIndexed { i, f -> TextButton(onClick = { open(i) }, modifier = Modifier.fillMaxWidth()) { Text(if (f.isBlueprint) "◇  " else "●  "+f.name.substringAfterLast('/'), color = if (i == selected) Cyan else TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 10.sp) } }
        Text("WORKSPACE VIEWS", color = TextMuted, fontSize = 8.sp, modifier = Modifier.padding(8.dp))
        TextButton(onClick = { tab("TERMINAL") }) { Text(">_  CLI Terminal", color = TextMuted, fontSize = 10.sp) }
        TextButton(onClick = { tab("AST") }) { Text("▣  AST Inspector", color = TextMuted, fontSize = 10.sp) }
        TextButton(onClick = { tab("TOKENS") }) { Text("▦  Token Inspector", color = TextMuted, fontSize = 10.sp) }
        Spacer(Modifier.height(12.dp)); Text("NUDL 0.3", color = Cyan, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
    }
}

@Composable
private fun Editor(state: NudlEditorState, mode: String, modifier: Modifier = Modifier, change: (String) -> Unit) {
    Column(modifier.fillMaxHeight().background(Editor).padding(7.dp)) {
        Row(Modifier.fillMaxWidth().height(36.dp).background(Panel2).border(1.dp, Border).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (mode == "TEXT") state.filename else "Graph.nblueprint", color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f)); Text(if (state.running) "● RUNNING" else "● READY", color = if (state.running) Amber else Green, fontSize = 9.sp)
        }
        Spacer(Modifier.height(5.dp))
        if (mode == "TEXT") OutlinedTextField(state.source, change, modifier = Modifier.fillMaxSize(), textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontFamily = FontFamily.Monospace), label = { Text("Noodle source") }) else Blueprint()
    }
}

@Composable
private fun Blueprint() {
    Box(Modifier.fillMaxSize().background(Color(0xFF081722)).border(1.dp, Border), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("VISUAL BLUEPRINT GRAPH", color = Cyan, fontWeight = FontWeight.Bold)
            Text("Nodes • connections • compile to Noodle", color = TextMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 6.dp))
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { Node("INPUT"); Text("→", color = Cyan); Node("FUNCTION"); Text("→", color = Cyan); Node("OUTPUT") }
            TextButton(onClick = {}) { Text("+ ADD NODE", color = Cyan, fontSize = 9.sp) }
        }
    }
}

@Composable private fun Node(text: String) = Box(Modifier.size(82.dp, 48.dp).background(Panel2, RoundedCornerShape(6.dp)).border(1.dp, Cyan, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) { Text(text, color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold) }

@Composable
private fun Inspector(state: NudlEditorState, target: String) {
    val errors = state.diagnostics.count { (it.severity ?: "").equals("error", true) }
    val warnings = state.diagnostics.count { (it.severity ?: "").equals("warning", true) }
    Column(Modifier.width(210.dp).fillMaxHeight().background(Panel).border(1.dp, Border).verticalScroll(rememberScrollState()).padding(10.dp)) {
        Text("INSPECTOR", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp)); Info("LANGUAGE", "NUDL 0.3"); Info("ERRORS", "$errors"); Info("WARNINGS", "$warnings"); Info("CODEGEN", if (state.generatedSource != null) "READY" else "IDLE"); Info("TARGET", target); Info("RUNTIME", "ANDROID")
        Text("PROPERTIES", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
        Text("Workspace: persistent", color = TextMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 7.dp))
        Text("API: NUDL service", color = TextMuted, fontSize = 10.sp)
    }
}

@Composable private fun Info(name: String, value: String) = Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text(name, color = TextMuted, fontSize = 9.sp, modifier = Modifier.weight(1f)); Text(value, color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold) }

@Composable
private fun BottomDock(tab: String, select: (String) -> Unit, state: NudlEditorState, terminal: List<String>, command: (String) -> Unit) {
    var input by remember { mutableStateOf("") }
    Column(Modifier.fillMaxWidth().height(170.dp).background(Color(0xFF07111F)).border(1.dp, Border)) {
        Row(Modifier.fillMaxWidth().height(34.dp).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
            listOf("TERMINAL", "OUTPUT", "PROBLEMS", "CONSOLE", "TARGET", "AST", "TOKENS").forEach { t -> TextButton(onClick = { select(t) }) { Text(t, color = if (t == tab) Cyan else TextMuted, fontSize = 9.sp) } }
        }
        Column(Modifier.weight(1f).padding(9.dp).verticalScroll(rememberScrollState())) {
            when (tab) {
                "TERMINAL" -> terminal.forEach { Text(it, color = if (it.startsWith("$")) Cyan else TextMuted, fontFamily = FontFamily.Monospace, fontSize = 10.sp) }
                "OUTPUT" -> Text("Noodle Compiler 0.3 — workspace ready.\nTarget: Android API backend", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                "PROBLEMS" -> Problems(state.diagnostics)
                "CONSOLE" -> Text(if (state.running) "Process running…" else "Console ready.", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                "TARGET" -> Text(state.generatedSource ?: "No generated target yet. Use Generate.", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                "AST" -> Text(astText(state.source), color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                "TOKENS" -> Text(tokenText(state.source), color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            }
        }
        if (tab == "TERMINAL") Row(Modifier.fillMaxWidth().height(38.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("$", color = Cyan, fontWeight = FontWeight.Bold); Spacer(Modifier.width(6.dp)); OutlinedTextField(input, { input = it }, modifier = Modifier.weight(1f), singleLine = true, textStyle = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontFamily = FontFamily.Monospace), placeholder = { Text("help | run | check | compile | ast | tokens | clear", fontSize = 9.sp) }); TextButton(onClick = { if (input.isNotBlank()) { command(input); input = "" } }) { Text("EXEC", color = Cyan, fontSize = 9.sp) }
        }
    }
}

@Composable private fun Problems(items: List<NudlDiagnostic>) {
    if (items.isEmpty()) Text("✓  No syntax or semantic problems detected.", color = Green, fontSize = 10.sp)
    else items.forEach { val severity = it.severity ?: "error"; Text("${severity.uppercase()}  ${it.message}", color = if (severity.equals("error", true)) Color(0xFFFF8080) else Amber, fontFamily = FontFamily.Monospace, fontSize = 10.sp) }
}

private fun astText(source: String): String {
    val lines = source.lines().filter { it.contains("fn ") || it.contains("struct ") || it.contains("return ") || it.contains("print(") }
    return if (lines.isEmpty()) "Program → empty AST" else "Program\n" + lines.joinToString("\n") { "  └─ ${it.trim()}" }
}

private fun tokenText(source: String): String {
    val tokens = Regex("[A-Za-z_][A-Za-z0-9_]*|[0-9]+|==|->|[{}()\\[\\],;:+*/=-]").findAll(source).map { it.value }.toList()
    return if (tokens.isEmpty()) "No tokens" else tokens.mapIndexed { i, t -> "%04d  %s".format(i, t) }.joinToString("\n")
}

private class NudlVmFactory(private val repository: NudlRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NudlViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return NudlViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
