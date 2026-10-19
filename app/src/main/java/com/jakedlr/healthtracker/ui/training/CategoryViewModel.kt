package com.jakedlr.healthtracker.ui.training

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jakedlr.healthtracker.appContainer
import com.jakedlr.healthtracker.data.TrainingRepository
import com.jakedlr.healthtracker.data.TrainingSaveResult
import com.jakedlr.healthtracker.data.db.ExerciseCategory
import com.jakedlr.healthtracker.data.db.ExerciseWithLastLogged
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoryUiState(
    val loading: Boolean = true,
    /** Null once loaded means the category no longer exists. */
    val category: ExerciseCategory? = null,
    val exercises: List<ExerciseWithLastLogged> = emptyList(),
)

enum class CategoryDeleteDialog { CONFIRM, NOT_EMPTY }

class CategoryViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: TrainingRepository,
) : ViewModel() {
    val categoryId: Long = checkNotNull(savedStateHandle[CATEGORY_ID_ARG])

    val uiState: StateFlow<CategoryUiState> = combine(
        repository.observeCategory(categoryId),
        repository.observeExercises(categoryId),
    ) { category, exercises -> CategoryUiState(loading = false, category = category, exercises = exercises) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoryUiState())

    private val _rename = MutableStateFlow<NameDialogState?>(null)
    val rename: StateFlow<NameDialogState?> = _rename.asStateFlow()

    private val _deleteDialog = MutableStateFlow<CategoryDeleteDialog?>(null)
    val deleteDialog: StateFlow<CategoryDeleteDialog?> = _deleteDialog.asStateFlow()

    fun openRename() = _rename.update { NameDialogState(text = uiState.value.category?.name.orEmpty()) }

    fun onRenameText(text: String) = _rename.update { it?.copy(text = text, nameTaken = false, blank = false) }

    fun dismissRename() = _rename.update { null }

    fun saveRename() {
        val dialog = _rename.value ?: return
        if (dialog.text.isBlank()) {
            _rename.update { it?.copy(blank = true) }
            return
        }
        viewModelScope.launch {
            when (repository.renameCategory(categoryId, dialog.text)) {
                is TrainingSaveResult.Saved -> _rename.update { null }
                else -> _rename.update { it?.copy(nameTaken = true) }
            }
        }
    }

    fun requestDelete() = _deleteDialog.update {
        if (uiState.value.exercises.isEmpty()) CategoryDeleteDialog.CONFIRM else CategoryDeleteDialog.NOT_EMPTY
    }

    fun dismissDelete() = _deleteDialog.update { null }

    /** The screen closes once the category is gone (it observes [uiState]). */
    fun confirmDelete() {
        viewModelScope.launch {
            val deleted = repository.deleteCategory(categoryId)
            _deleteDialog.update { if (deleted) null else CategoryDeleteDialog.NOT_EMPTY }
        }
    }

    fun reorder(exercises: List<ExerciseWithLastLogged>) {
        viewModelScope.launch { repository.reorderExercises(exercises.map { it.exercise.id }) }
    }

    companion object {
        const val CATEGORY_ID_ARG = "categoryId"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { CategoryViewModel(createSavedStateHandle(), appContainer().trainingRepository) }
        }
    }
}
