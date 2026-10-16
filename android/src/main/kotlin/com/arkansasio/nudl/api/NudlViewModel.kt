package com.arkansasio.nudl.api

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NudlViewModel(private val repository: NudlRepository) : ViewModel() {
    private val _state = MutableStateFlow(NudlEditorState())
    val state: StateFlow<NudlEditorState> = _state.asStateFlow()

    fun setSource(source: String) { _state.value = _state.value.copy(source = source) }
    fun analyze() { execute { repository.analyze(_state.value.source, _state.value.filename) } }
    fun run() { execute { repository.run(_state.value.source, _state.value.filename) } }

    fun generate(target: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(running = true)
            try {
                val response = repository.generate(_state.value.source, target, _state.value.filename)
                val generated = response.result as? Map<*, *>
                _state.value = _state.value.copy(
                    running = false,
                    diagnostics = response.diagnostics,
                    generatedSource = generated?.get("code") as? String,
                    generatedFilename = generated?.get("filename") as? String,
                )
            } catch (error: Exception) {
                _state.value = _state.value.copy(
                    running = false,
                    diagnostics = listOf(NudlDiagnostic(message = error.message ?: "API request failed", severity = "error")),
                )
            }
        }
    }

    private fun execute(request: suspend () -> NudlResponse<Any>) {
        viewModelScope.launch {
            _state.value = _state.value.copy(running = true)
            try {
                val response = request()
                _state.value = _state.value.copy(running = false, diagnostics = response.diagnostics)
            } catch (error: Exception) {
                _state.value = _state.value.copy(
                    running = false,
                    diagnostics = listOf(NudlDiagnostic(message = error.message ?: "API request failed", severity = "error")),
                )
            }
        }
    }
}
