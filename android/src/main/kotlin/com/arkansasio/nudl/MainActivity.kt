package com.arkansasio.nudl

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arkansasio.nudl.api.NudlRepository
import com.arkansasio.nudl.api.NudlServiceFactory
import com.arkansasio.nudl.api.NudlViewModel
import kotlinx.coroutines.delay

private const val BOOT_DURATION_MS = 1400L
private const val PRODUCT = "Noodle IDE"
private const val VERSION = "1.5.0-pre-alpha"
private const val BUILD_ID = "NUDL-NOODLE-1.5.0-PRE-ALPHA-TEST1"
private const val DEVELOPER = "Stephen Jr"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) { NoodleBootThenApp() }
            }
        }
    }
}

@Composable
private fun NoodleBootThenApp() {
    var booting by remember { mutableStateOf(true) }
    var progress by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        val steps = 14
        repeat(steps) { index ->
            delay(BOOT_DURATION_MS / steps)
            progress = (index + 1) / steps.toFloat()
        }
        booting = false
    }
    if (booting) NoodleBootScreen(progress)
    else {
        val repository = remember { NudlRepository(NudlServiceFactory.create()) }
        val vm: NudlViewModel = viewModel(factory = NudlViewModelFactory(repository))
        NUDLApp(vm)
    }
}

@Composable
private fun NoodleBootScreen(progress: Float) {
    val transition = rememberInfiniteTransition(label = "boot")
    val pulse by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "logoPulse"
    )
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF07111C)).padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Canvas(modifier = Modifier.height(150.dp).fillMaxWidth()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = 48f * pulse
                drawCircle(Color(0xFF46B8FF), radius, center, style = Stroke(7f))
                drawCircle(Color(0xFFB7E7FF), radius * 0.42f, center)
                drawLine(Color(0xFF46B8FF), Offset(center.x - radius * 1.45f, center.y), Offset(center.x + radius * 1.45f, center.y), 5f, cap = StrokeCap.Round)
            }
            Text(PRODUCT, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("Noodle bootloader", color = Color(0xFF8CB5CC), fontSize = 15.sp)
            Spacer(Modifier.height(20.dp))
            Text("BUILD $BUILD_ID", color = Color(0xFFB7E7FF), fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            Text("Starting Android runtime… ${(progress * 100).toInt()}%", color = Color(0xFF8CB5CC), fontSize = 13.sp)
        }
    }
}

@Composable
private fun NUDLApp(viewModel: NudlViewModel) {
    val state by viewModel.state.collectAsState()
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(PRODUCT, style = MaterialTheme.typography.headlineSmall)
        Text("$VERSION • Build 1 • Test 1 • $DEVELOPER")
        Text(BUILD_ID, style = MaterialTheme.typography.labelSmall)
        OutlinedTextField(value = state.source, onValueChange = viewModel::setSource, modifier = Modifier.fillMaxWidth(), minLines = 12, label = { Text(state.filename) })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = viewModel::analyze, enabled = !state.running) { Text("Analyze") }
            Button(onClick = viewModel::run, enabled = !state.running) { Text("Run") }
            Button(onClick = { viewModel.generate("typescript") }, enabled = !state.running) { Text("Generate TS") }
        }
        if (state.running) Text("Processing…")
        state.diagnostics.forEach { Text("${it.severity}: ${it.message}") }
        state.generatedSource?.let { Text("Generated source\n$it") }
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
