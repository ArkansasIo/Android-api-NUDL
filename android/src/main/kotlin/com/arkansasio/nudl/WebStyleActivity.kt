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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.arkansasio.nudl.api.NudlEditorState
import com.arkansasio.nudl.api.NudlRepository
import com.arkansasio.nudl.api.NudlServiceFactory
import com.arkansasio.nudl.api.NudlViewModel
import com.arkansasio.nudl.workspace.WorkspaceFile
import com.arkansasio.nudl.workspace.WorkspaceStore
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

private val WNavy = Color(0xFF07111C)
private val WPanel = Color(0xFF0B1522)
private val WPanel2 = Color(0xFF0D1B2A)
private val WEditor = Color(0xFF06101A)
private val WBorder = Color(0xFF20334A)
private val WBorder2 = Color(0xFF28425D)
private val WCyan = Color(0xFF6EE7FF)
private val WWhite = Color(0xFFE7F4FC)
private val WMuted = Color(0xFF71869B)
private val WGreen = Color(0xFF55D98A)
private val WAmber = Color(0xFFF1C75B)

class WebStyleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { Surface(Modifier.fillMaxSize(), color = WNavy) { WebStyleNoodleApp() } } }
    }
}

@Composable
private fun WebStyleNoodleApp() {
    var boot by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(450); boot = false }
    if (boot) WebBoot() else {
        val repository = remember { NudlRepository(NudlServiceFactory.create()) }
        val vm: NudlViewModel = viewModel(factory = NudlVmFactory(repository))
        WebWorkspace(vm)
    }
}

@Composable
private fun WebBoot() {
    Box(Modifier.fillMaxSize().background(WNavy), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(56.dp).background(Color(0xFF102238), RoundedCornerShape(8.dp)).border(1.dp, Color(0xFF3D647A), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Text("N", color = WCyan, fontSize = 30.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(10.dp))
            Text("Noodle IDE", color = WWhite, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Text("0.3  •  ANDROID EDITION", color = WCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun WebWorkspace(vm: NudlViewModel) {
    val context = LocalContext.current
    val store = remember { WorkspaceStore(context) }
    var files by remember { mutableStateOf(store.load()) }
    var selected by remember { mutableStateOf(0) }
    var mode by remember { mutableStateOf("text") }
    var target by remember { mutableStateOf("C++") }
    var sidebar by remember { mutableStateOf(true) }
    var rightPane by remember { mutableStateOf(true) }
    var bottom by remember { mutableStateOf("terminal") }
    var menu by remember { mutableStateOf<String?>(null) }
    var newDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var terminal by remember { mutableStateOf("$ nudlc --version\nNoodle Compiler 0.3 (Development Snapshot)\nTarget: C++23 Reference Backend\nType checking and diagnostics engine active.") }
    val state by vm.state.collectAsState()

    fun openFile(index: Int) {
        if (index !in files.indices) return
        selected = index
        val f = files[index]
        mode = if (f.isBlueprint || f.name.endsWith(".nblueprint")) "blueprint" else "text"
        vm.openFile(f.name, f.content)
    }
    fun saveSource(source: String) {
        if (selected !in files.indices || files[selected].readOnly) return
        val copy = files.toMutableList()
        copy[selected] = copy[selected].copy(content = source)
        files = copy
        store.save(copy)
    }
    fun newFile() {
        val name = newName.trim().replace('\\', '/')
        if (name.isBlank() || files.any { it.name.equals(name, true) }) return
        val bp = name.endsWith(".nblueprint")
        val source = if (bp) "// Visual Blueprint Graph: $name" else "fn main() -> i64 {\n    print(\"Hello from Noodle\");\n    return 0;\n}"
        val copy = (files + WorkspaceFile(name, source, bp)).toMutableList()
        files = copy
        store.save(copy)
        newDialog = false
        newName = ""
        openFile(copy.lastIndex)
    }

    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val root = JSONObject().apply {
                put("format", "NUDL-IDE-workspace"); put("version", 1)
                put("files", JSONArray().apply { files.forEach { f -> put(JSONObject().apply { put("name", f.name); put("content", f.content); put("isBlueprint", f.isBlueprint); put("readOnly", f.readOnly) }) } })
            }
            context.contentResolver.openOutputStream(uri)?.use { it.write(root.toString(2).toByteArray()) }
            terminal = "[Workspace] Exported ${files.size} file(s)."
            bottom = "output"
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) try {
            val root = JSONObject(context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: "")
            require(root.optString("format") == "NUDL-IDE-workspace")
            val a = root.getJSONArray("files")
            val copy = MutableList(a.length()) { i ->
                val f = a.getJSONObject(i)
                WorkspaceFile(f.getString("name"), f.optString("content"), f.optBoolean("isBlueprint"), f.optBoolean("readOnly"))
            }.toMutableList()
            if (copy.isNotEmpty()) { files = copy; store.save(copy); openFile(0); terminal = "[Workspace] Imported ${copy.size} file(s)."; bottom = "output" }
        } catch (_: Exception) { terminal = "[Workspace Import Error] Invalid workspace."; bottom = "problems" }
    }

    Column(Modifier.fillMaxSize().background(WNavy)) {
        WebTopbar(
            fileName = files.getOrNull(selected)?.name ?: "main.nudl",
            target = target,
            mode = mode,
            menu = menu,
            onMenu = { menu = if (menu == it) null else it },
            onDismiss = { menu = null },
            onNew = { menu = null; newDialog = true },
            onSave = { menu = null; saveSource(state.source); terminal = "[File] Saved \"${state.filename}\"." },
            onImport = { menu = null; import.launch(arrayOf("application/json", "text/json")) },
            onExport = { menu = null; export.launch("nudl-workspace.json") },
            onRun = { menu = null; vm.run(); bottom = "console"; terminal = "$ run ${state.filename}\nExecution requested." },
            onCompile = { menu = null; vm.generate(target.lowercase()); bottom = "target"; terminal = "$ nudlc ${state.filename} --target ${target.lowercase()}\nCompilation requested." },
            onFormat = { menu = null; val s = state.source.lines().joinToString("\n") { it.trimEnd() }; vm.setSource(s); saveSource(s); terminal = "[Format] Code formatted successfully." },
            onView = { requested -> menu = null; if (requested == "text") mode = "text" else if (requested == "blueprint") mode = "blueprint" },
            onTab = { requested -> menu = null; bottom = requested },
            onToggleSidebar = { menu = null; sidebar = !sidebar },
            onToggleRight = { menu = null; rightPane = !rightPane },
        )
        WebToolstrip(mode, target, onMode = { mode = it }, onNew = { newDialog = true }, onAnalyze = { vm.analyze(); bottom = "problems" }, onRun = { vm.run(); bottom = "console" }, onFormat = { val s = state.source.lines().joinToString("\n") { it.trimEnd() }; vm.setSource(s); saveSource(s) }, onCompile = { vm.generate(target.lowercase()); bottom = "target" }, onTarget = { target = it })
        Row(Modifier.weight(1f).fillMaxWidth()) {
            if (sidebar) WebSidebar(files, selected, onOpen = ::openFile, onNew = { newDialog = true }, onTab = { bottom = it })
            WebCenter(state, mode, target, Modifier.weight(1f), onSource = { vm.setSource(it); saveSource(it) })
            if (rightPane) WebInspector(state, target)
        }
        WebBottomDock(bottom, terminal, state, onTab = { bottom = it }) { command ->
            when (command.trim().lowercase()) {
                "run" -> { vm.run(); bottom = "console" }
                "check" -> { vm.analyze(); bottom = "problems" }
                "ast" -> bottom = "ast"
                "tokens" -> bottom = "tokens"
                "clear" -> terminal = "Noodle terminal cleared."
                "help" -> terminal = "help  run  check  compile  ast  tokens  clear"
                else -> if (command.trim().startsWith("compile")) { vm.generate(target.lowercase()); bottom = "target" } else terminal = "$ ${command.trim()}\nUnknown command. Type 'help'."
            }
        }
        Row(Modifier.fillMaxWidth().height(24.dp).background(WEditor).padding(horizontal = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (state.running) "● RUNNING" else "● READY", color = if (state.running) WAmber else WGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(14.dp)); Text("Problems ${state.diagnostics.size}", color = WMuted, fontSize = 8.sp)
            Spacer(Modifier.weight(1f)); Text("NUDL 0.3 • Android • $target", color = WMuted, fontSize = 8.sp)
        }
    }

    if (newDialog) AlertDialog(onDismissRequest = { newDialog = false }, title = { Text("New File") }, text = { OutlinedTextField(newName, { newName = it }, label = { Text("Path") }) }, confirmButton = { TextButton(onClick = ::newFile) { Text("CREATE") } }, dismissButton = { TextButton(onClick = { newDialog = false }) { Text("CANCEL") } })
}

@Composable
private fun WebTopbar(fileName: String, target: String, mode: String, menu: String?, onMenu: (String) -> Unit, onDismiss: () -> Unit, onNew: () -> Unit, onSave: () -> Unit, onImport: () -> Unit, onExport: () -> Unit, onRun: () -> Unit, onCompile: () -> Unit, onFormat: () -> Unit, onView: (String) -> Unit, onTab: (String) -> Unit, onToggleSidebar: () -> Unit, onToggleRight: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(56.dp).background(WPanel).border(1.dp, Color(0xFF18283A)).padding(horizontal = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(30.dp).background(Color(0xFF102238), RoundedCornerShape(5.dp)).border(1.dp, Color(0xFF3D647A), RoundedCornerShape(5.dp)), contentAlignment = Alignment.Center) { Text("N", color = WCyan, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.width(9.dp))
        Column(Modifier.width(165.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Text("Noodle IDE", color = WWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp); Spacer(Modifier.width(5.dp)); Text("0.3", color = WCyan, fontSize = 9.sp, fontFamily = FontFamily.Monospace) }; Text(fileName, color = WMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace) }
        MenuButton("File", menu, onMenu) {
            DropItem("New File…", onNew); DropItem("Save", onSave); DropDivider(); DropItem("Import Workspace (.json)", onImport); DropItem("Export Workspace (.json)", onExport)
        }
        MenuButton("Edit", menu, onMenu) { DropItem("Format Code", onFormat); DropItem("Clear Buffer", onDismiss) }
        MenuButton("Build", menu, onMenu) { DropItem("Run in Interpreter", onRun); DropItem("Compile to $target", onCompile); DropItem("Diagnostics & Problems", { onTab("problems") }) }
        MenuButton("View", menu, onMenu) { DropItem("Text Editor", { onView("text") }); DropItem("Blueprint Graph Editor", { onView("blueprint") }); DropDivider(); DropItem("Show Terminal", { onTab("terminal") }); DropItem("Show Target Output", { onTab("target") }) }
        MenuButton("Tools", menu, onMenu) { DropItem("Token Inspector", { onTab("tokens") }); DropItem("AST Inspector", { onTab("ast") }); DropItem("CLI Terminal", { onTab("terminal") }) }
        MenuButton("Help", menu, onMenu) { DropItem("Noodle 0.3 Documentation", { onTab("output") }); DropItem("About Noodle IDE", { onTab("output") }) }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onToggleSidebar) { Text(if (mode == "blueprint") "GRAPH" else "TREE", color = WCyan, fontSize = 9.sp) }
        TextButton(onClick = onToggleRight) { Text("INSPECTOR", color = WCyan, fontSize = 9.sp) }
    }
}

@Composable
private fun MenuButton(label: String, menu: String?, onMenu: (String) -> Unit, content: @Composable () -> Unit) {
    Box {
        TextButton(onClick = { onMenu(label) }) { Text("$label ▾", color = if (menu == label) WWhite else Color(0xFFD1DCE8), fontSize = 11.sp) }
        DropdownMenu(expanded = menu == label, onDismissRequest = { onMenu(label) }, modifier = Modifier.background(WPanel2).border(1.dp, WBorder2)) { content() }
    }
}

@Composable private fun DropItem(label: String, action: () -> Unit) { DropdownMenuItem(text = { Text(label, color = WWhite, fontSize = 11.sp) }, onClick = action) }
@Composable private fun DropDivider() { Spacer(Modifier.fillMaxWidth().height(1.dp).background(WBorder).padding(vertical = 3.dp)) }

@Composable
private fun WebToolstrip(mode: String, target: String, onMode: (String) -> Unit, onNew: () -> Unit, onAnalyze: () -> Unit, onRun: () -> Unit, onFormat: () -> Unit, onCompile: () -> Unit, onTarget: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().height(44.dp).background(WPanel2).border(1.dp, WBorder).horizontalScroll(rememberScrollState()).padding(horizontal = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        ToolButton(if (mode == "text") "CODE" else "BLUEPRINT") { onMode(if (mode == "text") "blueprint" else "text") }
        ToolButton("NEW", onNew); ToolButton("RUN", onRun); ToolButton("BUILD", onCompile); ToolButton("FORMAT", onFormat); ToolButton("ANALYZE", onAnalyze)
        Text("TARGET", color = WMuted, fontSize = 9.sp)
        listOf("C++", "TypeScript", "Python").forEach { t -> TextButton(onClick = { onTarget(t) }) { Text(t, color = if (target == t) WCyan else WMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace) } }
    }
}

@Composable private fun ToolButton(label: String, action: () -> Unit) = Button(onClick = action, modifier = Modifier.height(30.dp), shape = RoundedCornerShape(4.dp)) { Text(label, fontSize = 8.sp, fontWeight = FontWeight.Bold) }

@Composable
private fun WebSidebar(files: List<WorkspaceFile>, selected: Int, onOpen: (Int) -> Unit, onNew: () -> Unit, onTab: (String) -> Unit) {
    Column(Modifier.width(220.dp).fillMaxHeight().background(WPanel).border(1.dp, WBorder).verticalScroll(rememberScrollState()).padding(7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Text("SOLUTION EXPLORER", color = WMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); TextButton(onClick = onNew) { Text("+", color = WCyan) } }
        Text("WORKSPACE", color = WMuted, fontSize = 8.sp, modifier = Modifier.padding(7.dp))
        files.forEachIndexed { i, f -> TextButton(onClick = { onOpen(i) }, modifier = Modifier.fillMaxWidth()) { Text(if (i == selected) "▸  ${f.name}" else "   ${f.name}", color = if (i == selected) WCyan else WWhite, fontSize = 10.sp, fontFamily = FontFamily.Monospace) } }
        Spacer(Modifier.height(8.dp)); Text("VIEWS", color = WMuted, fontSize = 8.sp, modifier = Modifier.padding(7.dp))
        listOf("terminal" to "CLI Terminal", "output" to "Output", "problems" to "Problems", "ast" to "AST Inspector", "tokens" to "Token Inspector").forEach { (id, label) -> TextButton(onClick = { onTab(id) }) { Text("▣  $label", color = WMuted, fontSize = 10.sp) } }
    }
}

@Composable
private fun WebCenter(state: NudlEditorState, mode: String, target: String, modifier: Modifier, onSource: (String) -> Unit) {
    Column(modifier.fillMaxHeight().background(WEditor).padding(6.dp)) {
        Row(Modifier.fillMaxWidth().height(34.dp).background(WPanel2).border(1.dp, WBorder).padding(horizontal = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (mode == "text") state.filename else "Graph.nblueprint", color = WWhite, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f)); Text(target, color = WCyan, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.height(5.dp))
        if (mode == "text") {
            OutlinedTextField(value = state.source, onValueChange = onSource, modifier = Modifier.fillMaxSize(), textStyle = MaterialTheme.typography.bodyMedium.copy(color = WWhite, fontFamily = FontFamily.Monospace, fontSize = 12.sp), label = { Text("Noodle source") })
        } else {
            BlueprintSurface()
        }
    }
}

@Composable
private fun BlueprintSurface() {
    Box(Modifier.fillMaxSize().background(Color(0xFF081722)).border(1.dp, WBorder)) {
        Text("GRAPH  /  Noodle Blueprint", color = WMuted, fontSize = 9.sp, modifier = Modifier.padding(10.dp))
        Row(Modifier.fillMaxSize().padding(top = 45.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            GraphNode("INPUT", "source")
            Text("→", color = WCyan, fontSize = 22.sp)
            GraphNode("FUNCTION", "main()")
            Text("→", color = WCyan, fontSize = 22.sp)
            GraphNode("OUTPUT", "return")
        }
    }
}

@Composable private fun GraphNode(title: String, value: String) { Column(Modifier.width(105.dp).background(WPanel2, RoundedCornerShape(6.dp)).border(1.dp, Color(0xFF31516A), RoundedCornerShape(6.dp)).padding(8.dp)) { Text(title, color = WCyan, fontSize = 8.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(6.dp)); Text(value, color = WWhite, fontSize = 10.sp, fontFamily = FontFamily.Monospace) } }

@Composable
private fun WebInspector(state: NudlEditorState, target: String) {
    Column(Modifier.width(205.dp).fillMaxHeight().background(WPanel).border(1.dp, WBorder).verticalScroll(rememberScrollState()).padding(9.dp)) {
        Text("INSPECTOR", color = WMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp)); InspectorLine("FILE", state.filename); InspectorLine("TARGET", target); InspectorLine("STATE", if (state.running) "RUNNING" else "READY"); InspectorLine("DIAGNOSTICS", state.diagnostics.size.toString())
        Spacer(Modifier.height(12.dp)); Text("NODES", color = WMuted, fontSize = 8.sp); Text("Input", color = WWhite, fontSize = 10.sp); Text("Function", color = WWhite, fontSize = 10.sp); Text("Output", color = WWhite, fontSize = 10.sp)
    }
}
@Composable private fun InspectorLine(k: String, v: String) { Column(Modifier.padding(vertical = 4.dp)) { Text(k, color = WMuted, fontSize = 7.sp); Text(v, color = WWhite, fontSize = 10.sp, fontFamily = FontFamily.Monospace) } }

@Composable
private fun WebBottomDock(tab: String, terminal: String, state: NudlEditorState, onTab: (String) -> Unit, command: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().height(165.dp).background(WPanel).border(1.dp, WBorder)) {
        Row(Modifier.fillMaxWidth().height(32.dp).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
            listOf("terminal" to "TERMINAL", "output" to "OUTPUT", "problems" to "PROBLEMS", "console" to "CONSOLE", "target" to "TARGET", "ast" to "AST", "tokens" to "TOKENS").forEach { (id, label) -> TextButton(onClick = { onTab(id) }) { Text(label, color = if (tab == id) WCyan else WMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold) } }
        }
        if (tab == "terminal") {
            var input by remember { mutableStateOf("") }
            Column(Modifier.fillMaxSize().background(Color(0xFF050D15)).padding(9.dp)) {
                Text(terminal, color = WWhite, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                Row(verticalAlignment = Alignment.CenterVertically) { Text("$ ", color = WCyan, fontFamily = FontFamily.Monospace, fontSize = 9.sp); OutlinedTextField(input, { input = it }, modifier = Modifier.weight(1f).height(45.dp), textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)); TextButton(onClick = { command(input); input = "" }) { Text("EXEC", color = WCyan, fontSize = 8.sp) } }
            }
        } else {
            Column(Modifier.fillMaxSize().background(Color(0xFF050D15)).verticalScroll(rememberScrollState()).padding(9.dp)) {
                Text(when (tab) { "problems" -> "Diagnostics: ${state.diagnostics.size}"; "console" -> "Execution console"; "target" -> "Generated ${state.generatedFilename ?: "target output"}"; "ast" -> "AST Inspector"; "tokens" -> "Token Inspector"; else -> terminal }, color = WWhite, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                state.diagnostics.take(20).forEach { d -> Text("${d.severity ?: "error"}: ${d.message}", color = if (d.severity == "warning") WAmber else Color(0xFFFF7777), fontSize = 9.sp, modifier = Modifier.padding(top = 4.dp)) }
            }
        }
    }
}
