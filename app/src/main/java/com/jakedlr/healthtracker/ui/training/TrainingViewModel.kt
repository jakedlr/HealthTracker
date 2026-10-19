package com.jakedlr.healthtracker.ui.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jakedlr.healthtracker.appContainer
import com.jakedlr.healthtracker.data.TrainingRepository
import com.jakedlr.healthtracker.data.TrainingSaveResult
import com.jakedlr.healthtracker.data.db.ExerciseCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** An open category name dialog (new or rename): its text and why saving was refused, if it was. */
data class NameDialogState(val text: String = "", val nameTaken: Boolean = false, val blank: Boolean = false)

class TrainingViewModel(private val repository: TrainingRepository) : ViewModel() {
    /** Null until the first load, so the screen doesn't flash its empty state. */
    val categories: StateFlow<List<ExerciseCategory>?> = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _newCategory = MutableStateFlow<NameDialogState?>(null)
    val newCategory: StateFlow<NameDialogState?> = _newCategory.asStateFlow()

    fun openNewCategory() = _newCategory.update { NameDialogState() }

    fun onNewCategoryText(text: String) = _newCategory.update { it?.copy(text = text, nameTaken = false, blank = false) }

    fun dismissNewCategory() = _newCategory.update { null }

    fun createCategory() {
        val dialog = _newCategory.value ?: return
        if (dialog.text.isBlank()) {
            _newCategory.update { it?.copy(blank = true) }
            return
        }
        viewModelScope.launch {
            when (repository.createCategory(dialog.text)) {
                is TrainingSaveResult.Saved -> _newCategory.update { null }
                else -> _newCategory.update { it?.copy(nameTaken = true) }
            }
        }
    }

    fun reorder(categories: List<ExerciseCategory>) {
        viewModelScope.launch { repository.reorderCategories(categories.map { it.id }) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { TrainingViewModel(appContainer().trainingRepository) }
        }
    }
}
