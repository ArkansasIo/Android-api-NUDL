package com.arkansasio.nudl.api

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NudlViewModel(private val repository: NudlRepository) : ViewModel() {
    private val _state = MutableStateFlow(NudlEditorState())
    val state: StateFlow<NudlEditorState> = _state.asStateFlow()

    fun openFile(filename: String, source: String) {
        _state.value = NudlEditorState(filename = filename, source = source)
    }

    fun setSource(source: String) {
        _state.value = _state.value.copy(
            source = source,
            diagnostics = emptyList(),
            generatedSource = null,
            generatedFilename = null,
        )
    }

    fun lex() = execute { repository.lex(_state.value.source, _state.value.filename) }
    fun parse() = execute { repository.parse(_state.value.source, _state.value.filename) }
    fun analyze() = execute { repository.analyze(_state.value.source, _state.value.filename) }
    fun run() = execute { repository.run(_state.value.source, _state.value.filename) }

    fun generate(target: String) {
        execute {
            val snapshot = _state.value
            val response = repository.generate(snapshot.source, target, snapshot.filename)
            val generated = response.result as? Map<*, *>
            _state.value = _state.value.copy(
                diagnostics = response.diagnostics,
                generatedSource = generated?.get("code") as? String,
                generatedFilename = generated?.get("filename") as? String,
            )
            response
        }
    }

    private fun execute(request: suspend () -> NudlResponse<Any>) {
        viewModelScope.launch {
            _state.value = _state.value.copy(running = true, diagnostics = emptyList())
            try {
                val response = request()
                _state.value = _state.value.copy(
                    running = false,
                    diagnostics = if (response.ok) response.diagnostics else response.diagnostics.ifEmpty {
                        listOf(NudlDiagnostic(message = response.error ?: "Request failed", severity = "error"))
                    },
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.value = _state.value.copy(
                    running = false,
                    diagnostics = listOf(
                        NudlDiagnostic(
                            message = error.message ?: "API request failed",
                            severity = "error",
                        ),
                    ),
                )
            }
        }
    }
}
