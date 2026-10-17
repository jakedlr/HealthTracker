package com.jakedlr.healthtracker.ui.data

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jakedlr.healthtracker.data.BackupRepository
import com.jakedlr.healthtracker.data.DataSummary
import com.jakedlr.healthtracker.data.ImportResult
import com.jakedlr.healthtracker.data.InspectResult
import com.jakedlr.healthtracker.data.InvalidBackup
import com.jakedlr.healthtracker.domain.exportFileName
import com.jakedlr.healthtracker.healthTrackerApp
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DataTask { EXPORTING, CHECKING, IMPORTING }

data class DataUiState(
    val current: DataSummary? = null,
    val task: DataTask? = null,
    /** Result or error to show once. */
    val message: String? = null,
    /** A checked file waiting for the user to confirm replacing everything. */
    val pendingImport: InspectResult.Valid? = null,
    /** Import finished; the app should restart its screens on the new data. */
    val importFinished: Boolean = false,
)

class DataViewModel(
    private val backup: BackupRepository,
    private val clock: Clock,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DataUiState())
    val uiState: StateFlow<DataUiState> = _uiState.asStateFlow()

    init {
        refreshCurrent()
    }

    fun suggestedFileName(): String = exportFileName(LocalDate.now(clock))

    fun export(destination: Uri) = run(DataTask.EXPORTING) {
        val summary = backup.export(destination)
        _uiState.update { it.copy(message = "Exported ${describe(summary)}.") }
    }

    fun inspect(source: Uri) = run(DataTask.CHECKING) {
        when (val result = backup.inspect(source)) {
            is InspectResult.Valid -> _uiState.update { it.copy(pendingImport = result) }
            is InspectResult.Invalid -> _uiState.update { it.copy(message = explain(result.reason)) }
        }
    }

    fun cancelImport() {
        _uiState.value.pendingImport?.file?.delete()
        _uiState.update { it.copy(pendingImport = null) }
    }

    fun confirmImport() {
        val pending = _uiState.value.pendingImport ?: return
        _uiState.update { it.copy(pendingImport = null) }
        run(DataTask.IMPORTING) {
            when (backup.replaceWith(pending)) {
                ImportResult.Imported -> _uiState.update { it.copy(importFinished = true) }
                ImportResult.FailedAndRestored -> {
                    _uiState.update {
                        it.copy(message = "That file couldn't be opened, so nothing was changed. Your data is as it was.")
                    }
                    refreshCurrent()
                }
            }
        }
    }

    fun dismissMessage() = _uiState.update { it.copy(message = null) }

    private fun refreshCurrent() {
        viewModelScope.launch {
            val summary = runCatching { backup.currentSummary() }.getOrNull()
            _uiState.update { it.copy(current = summary) }
        }
    }

    private fun run(task: DataTask, block: suspend () -> Unit) {
        if (_uiState.value.task != null) return
        _uiState.update { it.copy(task = task) }
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "Something went wrong: ${e.message ?: e::class.simpleName}") }
            } finally {
                _uiState.update { it.copy(task = null) }
            }
        }
    }

    private fun explain(reason: InvalidBackup): String = when (reason) {
        InvalidBackup.NOT_SQLITE -> "That file isn't a Health Tracker export."
        InvalidBackup.NEWER_VERSION -> "That file comes from a newer version of the app and can't be imported here."
        InvalidBackup.UNKNOWN_VERSION, InvalidBackup.MISSING_TABLES -> "That file isn't a Health Tracker export."
        InvalidBackup.CORRUPT -> "That file is damaged and can't be imported."
        InvalidBackup.UNREADABLE -> "That file couldn't be read."
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = healthTrackerApp()
                DataViewModel(app.backupRepository, app.container.clock)
            }
        }
    }
}

/** e.g. "412 weigh-ins, 1,830 food logs, 260 sleep entries". */
fun describe(s: DataSummary): String =
    "%,d weigh-ins, %,d food logs, %,d sleep entries".format(s.weighIns, s.foodLogs, s.sleepEntries)
