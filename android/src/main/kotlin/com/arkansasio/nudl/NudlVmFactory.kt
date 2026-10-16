package com.arkansasio.nudl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.arkansasio.nudl.api.NudlRepository
import com.arkansasio.nudl.api.NudlViewModel

/**
 * Shared ViewModel factory for Noodle Android activities.
 *
 * MainActivity historically contained a file-private copy of this factory.
 * WebStyleActivity is a different Kotlin source file, so that private symbol
 * was not accessible during compilation. Keeping this shared factory public
 * allows all activities in this package to construct NudlViewModel instances.
 */
class NudlVmFactory(private val repository: NudlRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NudlViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NudlViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
