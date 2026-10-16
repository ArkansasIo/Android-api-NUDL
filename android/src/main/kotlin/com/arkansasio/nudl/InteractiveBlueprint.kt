package com.arkansasio.nudl

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

data class AndroidBpPin(val id: String, val name: String, val input: Boolean)
data class AndroidBpNode(val id: String, val title: String, val type: String, val x: Float, val y: Float, val inputs: List<AndroidBpPin>, val outputs: List<AndroidBpPin>)
data class AndroidBpWire(val from: String, val to: String)

private val BpBg = Color(0xFF07111F)
private val BpGrid = Color(0xFF1C2D3E)
private val BpNode = Color(0xFF102033)
private val BpBorder = Color(0xFF28425D)
private val BpCyan = Color(0xFF6EE7FF)
private val BpText = Color(0xFFE7F4FC)

@Composable
fun InteractiveBlueprintSurface(onCompile: (String) -> Unit) {
    var nodes by remember { mutableStateOf(defaultBpNodes()) }
    var wires by remember { mutableStateOf(listOf(AndroidBpWire("begin.out", "main.in"), AndroidBpWire("main.out", "return.in"))) }
    var selected by remember { mutableStateOf<String?>(null) }
    var connectFrom by remember { mutableStateOf<String?>(null) }
    var pan by remember { mutableStateOf(Offset.Zero) }

    Column(Modifier.fillMaxSize().background(BpBg)) {
        Row(Modifier.fillMaxWidth().height(40.dp).background(Color(0xFF0B1827)).padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("VISUAL BLUEPRINT GRAPH", color = BpText, fontSize = 10.sp, modifier = Modifier.padding(top = 11.dp))
            Spacer(Modifier.weight(1f))
            Button(onClick = { nodes = defaultBpNodes(); wires = listOf(AndroidBpWire("begin.out", "main.in"), AndroidBpWire("main.out", "return.in")); selected = null }) { Text("RESET", fontSize = 8.sp) }
            Button(onClick = { onCompile(compileAndroidBlueprint(nodes, wires)) }) { Text("COMPILE TO NOODLE", color = BpCyan, fontSize = 8.sp) }
        }
        Row(Modifier.fillMaxSize()) {
            Column(Modifier.width(150.dp).fillMaxHeight().background(Color(0xFF091624)).padding(8.dp).verticalScroll(rememberScrollState())) {
                Text("NODE PALETTE", color = BpCyan, fontSize = 9.sp)
                listOf("Event" to "event", "Function" to "function", "Math" to "math", "Variable" to "variable", "Logic" to "logic", "Flow" to "flow").forEach { (title, type) ->
                    Button(onClick = { nodes = nodes + newBpNode(title, type, nodes.size) }, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) { Text("+ $title", fontSize = 8.sp) }
                }
                Text("Click an output pin, then an input pin to connect. Drag node headers to move them.", color = Color(0xFF71869B), fontSize = 8.sp, modifier = Modifier.padding(top = 10.dp))
            }
            Box(Modifier.fillMaxSize().horizontalScroll(rememberScrollState()).verticalScroll(rememberScrollState())) {
                Box(Modifier.width(2200.dp).height(1400.dp).background(BpBg)) {
                    Canvas(Modifier.fillMaxSize().pointerInput(Unit) {
                        detectTapGestures(onTap = { p ->
                            val hit = nodes.lastOrNull { p.x >= it.x && p.x <= it.x + 220 && p.y >= it.y && p.y <= it.y + 105 }
                            selected = hit?.id
                        })
                    }.pointerInput(nodes) {
                        detectDragGestures(onDragEnd = { }, onDragCancel = { }, onDrag = { change, delta ->
                            val p = change.position
                            val hit = nodes.lastOrNull { p.x >= it.x && p.x <= it.x + 220 && p.y >= it.y && p.y <= it.y + 35 }
                            if (hit != null) {
                                change.consume()
                                nodes = nodes.map { n -> if (n.id == hit.id) n.copy(x = max(5f, n.x + delta.x), y = max(5f, n.y + delta.y)) else n }
                            }
                        })
                    }) {
                        for (x in 0..2200 step 24) drawCircle(BpGrid, 1.5f, Offset(x.toFloat(), 0f))
                        for (y in 0..1400 step 24) drawCircle(BpGrid, 1.5f, Offset(0f, y.toFloat()))
                        wires.forEach { wire ->
                            val from = wire.from.substringBefore('.')
                            val to = wire.to.substringBefore('.')
                            val a = nodes.find { it.id == from }
                            val b = nodes.find { it.id == to }
                            if (a != null && b != null) {
                                drawLine(BpCyan, Offset(a.x + 220, a.y + 55), Offset(b.x, b.y + 55), strokeWidth = 3f)
                            }
                        }
                    }
                    nodes.forEach { node ->
                        BpNodeCard(node, node.id == selected, connectFrom, onSelect = { selected = node.id }, onPin = { pin ->
                            val endpoint = "${node.id}.${pin.id}"
                            if (connectFrom == null) connectFrom = endpoint else {
                                if (connectFrom!!.substringBefore('.') != node.id && connectFrom!!.substringAfter('.') != pin.id) wires = wires + AndroidBpWire(connectFrom!!, endpoint)
                                connectFrom = null
                            }
                        })
                    }
                }
            }
        }
    }
}

@Composable private fun BpNodeCard(node: AndroidBpNode, selected: Boolean, connecting: String?, onSelect: () -> Unit, onPin: (AndroidBpPin) -> Unit) {
    Box(Modifier.offset(node.x.dp, node.y.dp).width(220.dp).border(if (selected) 2.dp else 1.dp, if (selected) BpCyan else BpBorder).background(BpNode).pointerInput(node.id) { detectTapGestures(onTap = { onSelect() }) }) {
        Column(Modifier.padding(8.dp)) {
            Text(node.title, color = BpCyan, fontSize = 10.sp)
            Text(node.type.uppercase(), color = Color(0xFF71869B), fontSize = 7.sp)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column { node.inputs.forEach { p -> PinText(p, connecting, onPin) } }
                Column { node.outputs.forEach { p -> PinText(p, connecting, onPin) } }
            }
        }
    }
}

@Composable private fun PinText(pin: AndroidBpPin, connecting: String?, onPin: (AndroidBpPin) -> Unit) {
    Text(if (pin.input) "● ${pin.name}" else "${pin.name} ●", color = if (connecting != null) BpCyan else Color(0xFFA9C5DC), fontSize = 8.sp, modifier = Modifier.padding(vertical = 2.dp).pointerInput(pin.id) { detectTapGestures(onTap = { onPin(pin) }) })
}

private fun defaultBpNodes() = listOf(
    AndroidBpNode("begin", "Begin Play", "event", 100f, 180f, emptyList(), listOf(AndroidBpPin("out", "exec", false))),
    AndroidBpNode("main", "Main Function", "function", 520f, 180f, listOf(AndroidBpPin("in", "exec", true)), listOf(AndroidBpPin("out", "exec", false))),
    AndroidBpNode("return", "Return", "flow", 940f, 180f, listOf(AndroidBpPin("in", "exec", true)), emptyList())
)

private fun newBpNode(title: String, type: String, index: Int) = AndroidBpNode("node-${System.currentTimeMillis()}", title, type, 160f + (index % 4) * 280f, 380f + (index / 4) * 160f, listOf(AndroidBpPin("in", "in", true)), listOf(AndroidBpPin("out", "out", false)))

private fun compileAndroidBlueprint(nodes: List<AndroidBpNode>, wires: List<AndroidBpWire>): String {
    val body = nodes.filter { it.type == "function" || it.type == "math" || it.type == "variable" || it.type == "logic" }.joinToString("\n") { "    // ${it.title} (${it.type})" }
    return "fn main() -> i64 {\n$body\n    return 0;\n}"
}
