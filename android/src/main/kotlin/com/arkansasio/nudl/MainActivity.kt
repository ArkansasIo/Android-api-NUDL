package com.arkansasio.nudl

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arkansasio.nudl.api.NudlRepository
import com.arkansasio.nudl.api.NudlServiceFactory
import com.arkansasio.nudl.api.NudlViewModel
import kotlinx.coroutines.delay

private const val PRODUCT = "Noodle IDE"
private const val VERSION = "1.5.0-pre-alpha"
private const val BUILD_ID = "NUDL-NOODLE-1.5.0-PRE-ALPHA-TEST1"
private const val DEVELOPER = "Stephen Jr"

private val Navy = Color(0xFF07111C)
private val Panel = Color(0xFF0B1824)
private val Panel2 = Color(0xFF0F202E)
private val Border = Color(0xFF1C3446)
private val Cyan = Color(0xFF46B8FF)
private val TextPrimary = Color(0xFFE7F4FC)
private val TextMuted = Color(0xFF7F9CAE)
private val Green = Color(0xFF55D98A)
private val Amber = Color(0xFFF1C75B)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Navy) { NoodleBootThenApp() }
            }
        }
    }
}

@Composable
private fun NoodleBootThenApp() {
    var booting by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(900); booting = false }
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
            Spacer(Modifier.height(22.dp))
            Text("$VERSION  •  $BUILD_ID", color = TextMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun NoodleWorkspace(viewModel: NudlViewModel) {
    val state by viewModel.state.collectAsState()
    var sidebar by remember { mutableStateOf(true) }
    var rightPane by remember { mutableStateOf(true) }
    var bottomTab by remember { mutableStateOf("TERMINAL") }
    var target by remember { mutableStateOf("C++") }
    var mode by remember { mutableStateOf("TEXT") }
    var selectedFile by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize().background(Navy)) {
        TopBar(sidebar, { sidebar = !sidebar }, rightPane, { rightPane = !rightPane })
        ToolStrip(
            mode = mode,
            onMode = { mode = it },
            target = target,
            onTarget = { target = it },
            analyze = viewModel::analyze,
            run = viewModel::run,
            generate = { viewModel.generate("typescript") }
        )
        Row(Modifier.weight(1f).fillMaxWidth()) {
            if (sidebar) SidebarPanel(selectedFile) { selectedFile = it }
            EditorPanel(state.source, state.filename, viewModel::setSource, state.running, mode)
            if (rightPane) InspectorPanel(state.diagnostics.size, state.generatedSource != null)
        }
        BottomDock(bottomTab, { bottomTab = it }, state)
        StatusBar(state.running, state.diagnostics.size, target)
    }
}

@Composable
private fun TopBar(sidebar: Boolean, toggleSidebar: () -> Unit, rightPane: Boolean, toggleRight: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(52.dp).background(Panel).border(1.dp, Border).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("N", color = Cyan, fontSize = 25.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.width(10.dp))
        Text(PRODUCT, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.width(8.dp))
        Text("/ WORKSPACE", color = TextMuted, fontSize = 12.sp)
        Spacer(Modifier.weight(1f))
        Text("$VERSION", color = TextMuted, fontSize = 11.sp)
        Spacer(Modifier.width(12.dp))
        TextButton(onClick = toggleSidebar) { Text(if (sidebar) "HIDE TREE" else "TREE", color = Cyan, fontSize = 11.sp) }
        TextButton(onClick = toggleRight) { Text(if (rightPane) "HIDE INSPECTOR" else "INSPECTOR", color = Cyan, fontSize = 11.sp) }
    }
}

@Composable
private fun ToolStrip(mode: String, onMode: (String) -> Unit, target: String, onTarget: (String) -> Unit, analyze: () -> Unit, run: () -> Unit, generate: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(48.dp).background(Panel2).border(1.dp, Border).horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ToolButton(if (mode == "TEXT") "TEXT" else "BLUEPRINT") { onMode(if (mode == "TEXT") "BLUEPRINT" else "TEXT") }
        ToolButton("ANALYZE", analyze)
        ToolButton("RUN", run)
        ToolButton("FORMAT") { }
        ToolButton("GENERATE TS", generate)
        Spacer(Modifier.width(8.dp))
        Text("TARGET", color = TextMuted, fontSize = 10.sp)
        listOf("C++", "TypeScript", "Python").forEach { item ->
            TextButton(onClick = { onTarget(item) }) { Text(item, color = if (target == item) Cyan else TextMuted, fontSize = 11.sp) }
        }
    }
}

@Composable
private fun ToolButton(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, shape = RoundedCornerShape(4.dp), modifier = Modifier.height(34.dp)) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SidebarPanel(selected: Int, onSelect: (Int) -> Unit) {
    Column(Modifier.width(190.dp).fillMaxHeight().background(Panel).border(1.dp, Border).verticalScroll(rememberScrollState()).padding(10.dp)) {
        Text("EXPLORER", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("▾ NUDL WORKSPACE", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        val files = listOf("main.nudl", "math.nudl", "Graph.nblueprint", "README.md")
        files.forEachIndexed { index, file ->
            TextButton(onClick = { onSelect(index) }, modifier = Modifier.fillMaxWidth()) {
                Text(if (index == selected) "▸ $file" else "  $file", color = if (index == selected) Cyan else TextMuted, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("WORKSPACE", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        listOf("New File", "Import", "Export", "Reset").forEach { TextButton(onClick = {}) { Text(it, color = TextMuted, fontSize = 11.sp) } }
    }
}

@Composable
private fun EditorPanel(source: String, filename: String, onSourceChange: (String) -> Unit, running: Boolean, mode: String) {
    Column(Modifier.weight(1f).fillMaxHeight().background(Color(0xFF06101A)).padding(8.dp)) {
        Row(Modifier.fillMaxWidth().height(36.dp).background(Panel2).border(1.dp, Border).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (mode == "TEXT") filename else "Graph.nblueprint", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(if (running) "● RUNNING" else "● READY", color = if (running) Amber else Green, fontSize = 10.sp)
        }
        Spacer(Modifier.height(6.dp))
        if (mode == "TEXT") {
            OutlinedTextField(
                value = source,
                onValueChange = onSourceChange,
                modifier = Modifier.fillMaxSize(),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                label = { Text("Noodle source", color = TextMuted) }
            )
        } else {
            BlueprintSurface()
        }
    }
}

@Composable
private fun BlueprintSurface() {
    Box(Modifier.fillMaxSize().background(Color(0xFF081722)).border(1.dp, Border), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("BLUEPRINT GRAPH", color = Cyan, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            Text("START  →  FUNCTION  →  OUTPUT", color = TextMuted, fontSize = 11.sp)
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Node("INPUT")
                Text("→", color = Cyan, fontSize = 20.sp)
                Node("PROCESS")
                Text("→", color = Cyan, fontSize = 20.sp)
                Node("OUTPUT")
            }
        }
    }
}

@Composable
private fun Node(label: String) {
    Box(Modifier.size(width = 82.dp, height = 48.dp).background(Panel2, RoundedCornerShape(6.dp)).border(1.dp, Cyan, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
        Text(label, color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InspectorPanel(errors: Int, generated: Boolean) {
    Column(Modifier.width(205.dp).fillMaxHeight().background(Panel).border(1.dp, Border).padding(12.dp)) {
        Text("INSPECTOR", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Info("LANGUAGE", "NUDL 0.3")
        Info("DIAGNOSTICS", "$errors")
        Info("CODEGEN", if (generated) "READY" else "IDLE")
        Info("RUNTIME", "ANDROID")
        Spacer(Modifier.height(18.dp))
        Text("PROPERTIES", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text("Editor", color = TextPrimary, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        Text("Syntax analysis: live", color = TextMuted, fontSize = 11.sp)
        Text("Workspace: local", color = TextMuted, fontSize = 11.sp)
    }
}

@Composable
private fun Info(name: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(name, color = TextMuted, fontSize = 9.sp, modifier = Modifier.weight(1f))
        Text(value, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BottomDock(tab: String, select: (String) -> Unit, state: com.arkansasio.nudl.api.NudlEditorState) {
    Column(Modifier.fillMaxWidth().height(150.dp).background(Color(0xFF07131E)).border(1.dp, Border)) {
        Row(Modifier.fillMaxWidth().height(34.dp).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
            listOf("TERMINAL", "OUTPUT", "PROBLEMS", "CONSOLE", "TARGET").forEach { item ->
                TextButton(onClick = { select(item) }) { Text(item, color = if (tab == item) Cyan else TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            }
        }
        val body = when (tab) {
            "PROBLEMS" -> if (state.diagnostics.isEmpty()) "No problems detected." else state.diagnostics.joinToString("\n") { "${it.severity}: ${it.message}" }
            "TARGET" -> state.generatedSource ?: "No generated target yet."
            "CONSOLE" -> if (state.running) "Process running…" else "Console ready."
            else -> if (state.running) "$ run ${state.filename}\nProcess running…" else "$ nudlc ${state.filename}\nNoodle Compiler 0.3 — workspace ready."
        }
        Text(body, color = TextMuted, fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState()))
    }
}

@Composable
private fun StatusBar(running: Boolean, diagnostics: Int, target: String) {
    Row(Modifier.fillMaxWidth().height(26.dp).background(Color(0xFF06101A)).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(if (running) "RUNNING" else "READY", color = if (running) Amber else Green, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(14.dp))
        Text("Problems $diagnostics", color = TextMuted, fontSize = 9.sp)
        Spacer(Modifier.weight(1f))
        Text("NUDL • $target • Android • $DEVELOPER", color = TextMuted, fontSize = 9.sp)
    }
}

private class NudlViewModelFactory(private val repository: NudlRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NudlViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return NudlViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
