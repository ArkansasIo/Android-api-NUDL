package com.arkansasio.nudl

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arkansasio.nudl.api.NudlEditorState
import com.arkansasio.nudl.api.NudlRepository
import com.arkansasio.nudl.api.NudlServiceFactory
import com.arkansasio.nudl.api.NudlViewModel
import kotlin.math.roundToInt

private val P0 = Color(0xFF07111C); private val P1 = Color(0xFF0B1522); private val P2 = Color(0xFF0D1B2A)
private val PE = Color(0xFF06101A); private val PB = Color(0xFF20334A); private val CY = Color(0xFF6EE7FF)
private val WT = Color(0xFFE7F4FC); private val MU = Color(0xFF71869B); private val GR = Color(0xFF55D98A)

class NoodleWebParityActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { MaterialTheme { NoodleParityApp() } } }
}

data class ParityNode(val id: String, val title: String, val type: String, val x: Float, val y: Float)

auto: @Composable
private fun NoodleParityApp() {
    val repo = remember { NudlRepository(NudlServiceFactory.create()) }
    val vm: NudlViewModel = viewModel(factory = NudlVmFactory(repo))
    var blueprint by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf("main.nudl") }
    var sidebar by remember { mutableStateOf(true) }
    var inspector by remember { mutableStateOf(true) }
    var bottom by remember { mutableStateOf("TERMINAL") }
    var target by remember { mutableStateOf("C++") }
    var nodes by remember { mutableStateOf(parityNodes()) }
    val state by vm.state.collectAsState()

    Column(Modifier.fillMaxSize().background(P0)) {
        Row(Modifier.fillMaxWidth().height(56.dp).background(P1).border(1.dp, PB).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).background(P2, RoundedCornerShape(5.dp)).border(1.dp, CY, RoundedCornerShape(5.dp)), contentAlignment = Alignment.Center) { Text("N", color = CY, fontWeight = FontWeight.Black) }
            Spacer(Modifier.width(8.dp)); Column(Modifier.width(145.dp)) { Text("Noodle IDE", color = WT, fontWeight = FontWeight.Bold, fontSize = 13.sp); Text("0.3 • ANDROID", color = CY, fontSize = 8.sp, fontFamily = FontFamily.Monospace) }
            listOf("File", "Edit", "Build", "View", "Tools", "Help").forEach { TextButton(onClick = { bottom = it.uppercase() }) { Text("$it ▾", color = WT, fontSize = 10.sp) } }
            Spacer(Modifier.weight(1f)); TextButton(onClick = { sidebar = !sidebar }) { Text("TREE", color = CY, fontSize = 9.sp) }; TextButton(onClick = { inspector = !inspector }) { Text("INSPECTOR", color = CY, fontSize = 9.sp) }
        }
        Row(Modifier.fillMaxWidth().height(42.dp).background(P2).border(1.dp, PB).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
            Tool("CODE") { blueprint = false }; Tool("BLUEPRINT") { blueprint = true }; Tool("NEW") {}; Tool("RUN") { vm.run(); bottom = "CONSOLE" }; Tool("BUILD") { vm.generate(target.lowercase()); bottom = "TARGET" }; Tool("ANALYZE") { vm.analyze(); bottom = "PROBLEMS" }
            Text("TARGET", color = MU, fontSize = 8.sp, modifier = Modifier.padding(horizontal = 6.dp)); listOf("C++", "TypeScript", "Python").forEach { t -> TextButton(onClick = { target = t }) { Text(t, color = if (target == t) CY else MU, fontSize = 8.sp, fontFamily = FontFamily.Monospace) } }
        }
        Row(Modifier.weight(1f).fillMaxWidth()) {
            if (sidebar) Column(Modifier.width(205.dp).fillMaxHeight().background(P1).border(1.dp, PB).verticalScroll(rememberScrollState()).padding(8.dp)) {
                Text("SOLUTION EXPLORER", color = MU, fontSize = 9.sp, fontWeight = FontWeight.Bold); Text("WORKSPACE", color = MU, fontSize = 8.sp, modifier = Modifier.padding(vertical = 9.dp));
                listOf("main.nudl", "math.nudl", "Graph.nblueprint", "README.md").forEach { f -> TextButton(onClick = { selected = f; blueprint = f.endsWith(".nblueprint") }) { Text(if (selected == f) "▸ $f" else "  $f", color = if (selected == f) CY else WT, fontFamily = FontFamily.Monospace, fontSize = 9.sp) } }
                Text("VIEWS", color = MU, fontSize = 8.sp, modifier = Modifier.padding(vertical = 9.dp)); listOf("TERMINAL", "OUTPUT", "PROBLEMS", "CONSOLE", "TARGET", "AST", "TOKENS").forEach { t -> TextButton(onClick = { bottom = t }) { Text("▣ $t", color = MU, fontSize = 9.sp) } }
            }
            if (blueprint) ParityBlueprint(nodes, { nodes = it }, Modifier.weight(1f)) else ParityTextEditor(state, vm, Modifier.weight(1f))
            if (inspector) Column(Modifier.width(190.dp).fillMaxHeight().background(P1).border(1.dp, PB).padding(9.dp)) {
                Text("INSPECTOR", color = MU, fontSize = 9.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(12.dp)); Info("FILE", selected); Info("TARGET", target); Info("STATE", if (state.running) "RUNNING" else "READY"); Info("DIAGNOSTICS", state.diagnostics.size.toString()); Spacer(Modifier.height(12.dp)); Text("BLUEPRINT", color = MU, fontSize = 8.sp); Text("${nodes.size} nodes", color = WT, fontSize = 10.sp); Text("${if (blueprint) "interactive" else "text mode"}", color = CY, fontSize = 9.sp)
            }
        }
        Column(Modifier.fillMaxWidth().height(145.dp).background(P1).border(1.dp, PB)) {
            Row(Modifier.fillMaxWidth().height(31.dp).horizontalScroll(rememberScrollState())) { listOf("TERMINAL", "OUTPUT", "PROBLEMS", "CONSOLE", "TARGET", "AST", "TOKENS").forEach { t -> TextButton(onClick = { bottom = t }) { Text(t, color = if (bottom == t) CY else MU, fontSize = 8.sp, fontWeight = FontWeight.Bold) } } }
            Text(when (bottom) { "PROBLEMS" -> "Diagnostics: ${state.diagnostics.size}"; "TARGET" -> "Generated: ${state.generatedFilename ?: "none"}"; "CONSOLE" -> "Interpreter execution requested."; "AST" -> "AST Inspector"; "TOKENS" -> "Token Inspector"; else -> "$ nudlc --version\nNoodle Compiler 0.3 • Android backend\nTarget: $target" }, color = WT, fontFamily = FontFamily.Monospace, fontSize = 9.sp, modifier = Modifier.padding(9.dp))
        }
        Row(Modifier.fillMaxWidth().height(23.dp).background(PE).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) { Text("● ${if (state.running) "RUNNING" else "READY"}", color = if (state.running) Color(0xFFF1C75B) else GR, fontSize = 8.sp); Spacer(Modifier.weight(1f)); Text("NUDL 0.3 • Android • $target", color = MU, fontSize = 8.sp) }
    }
}

@Composable private fun Tool(label: String, action: () -> Unit) { TextButton(onClick = action) { Text(label, color = CY, fontSize = 8.sp, fontWeight = FontWeight.Bold) } }
@Composable private fun Info(k: String, v: String) { Text(k, color = MU, fontSize = 7.sp); Text(v, color = WT, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(bottom = 7.dp)) }

@Composable private fun ParityTextEditor(state: NudlEditorState, vm: NudlViewModel, modifier: Modifier) { Column(modifier.fillMaxHeight().background(PE).padding(6.dp)) { Row(Modifier.fillMaxWidth().height(33.dp).background(P2).border(1.dp, PB).padding(8.dp), verticalAlignment = Alignment.CenterVertically) { Text(state.filename, color = WT, fontFamily = FontFamily.Monospace, fontSize = 9.sp); Spacer(Modifier.weight(1f)); Text("C++", color = CY, fontSize = 8.sp) }; Spacer(Modifier.height(5.dp)); OutlinedTextField(state.source, { vm.setSource(it) }, modifier = Modifier.fillMaxSize(), textStyle = MaterialTheme.typography.bodySmall.copy(color = WT, fontFamily = FontFamily.Monospace, fontSize = 12.sp), label = { Text("Noodle source") }) } }

@Composable private fun ParityBlueprint(nodes: List<ParityNode>, onNodes: (List<ParityNode>) -> Unit, modifier: Modifier) {
    BoxWithConstraints(modifier.fillMaxHeight().background(PE).border(1.dp, PB)) {
        Canvas(Modifier.fillMaxSize()) { val step = 24f; var x = 0f; while (x < size.width) { var y = 0f; while (y < size.height) { drawCircle(Color(0xFF1C2D3E), 1.3f, Offset(x, y)); y += step }; x += step }; val pairs = listOf(0 to 1, 1 to 2); pairs.forEach { (a,b) -> if (a < nodes.size && b < nodes.size) drawLine(CY, Offset(nodes[a].x + 180, nodes[a].y + 48), Offset(nodes[b].x, nodes[b].y + 48), strokeWidth = 3f) } }
        nodes.forEachIndexed { i, node ->
            Column(Modifier.offset { IntOffset(node.x.roundToInt(), node.y.roundToInt()) }.width(180.dp).background(P2, RoundedCornerShape(6.dp)).border(1.dp, if (i == 1) CY else PB, RoundedCornerShape(6.dp)).pointerInput(node.id) { detectDragGestures { change, drag -> change.consume(); onNodes(nodes.mapIndexed { j,n -> if (j == i) n.copy(x = (n.x + drag.x).coerceAtLeast(0f), y = (n.y + drag.y).coerceAtLeast(0f)) else n }) } }.padding(8.dp)) {
                Text(node.title, color = CY, fontWeight = FontWeight.Bold, fontSize = 9.sp); Text(node.type.uppercase(), color = MU, fontSize = 7.sp); Spacer(Modifier.height(8.dp)); Text("● input", color = WT, fontSize = 8.sp); Text("output ●", color = WT, fontSize = 8.sp)
            }
        }
        Row(Modifier.align(Alignment.TopStart).padding(8.dp).background(P1.copy(alpha = .92f)).padding(6.dp)) { Text("VISUAL BLUEPRINT GRAPH  •  Drag nodes", color = MU, fontSize = 8.sp) }
    }
}

private fun parityNodes() = listOf(ParityNode("begin", "Begin Play", "event", 80f, 150f), ParityNode("main", "Main Function", "function", 360f, 150f), ParityNode("return", "Return", "flow", 640f, 150f))
