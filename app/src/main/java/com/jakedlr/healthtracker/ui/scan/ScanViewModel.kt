package com.jakedlr.healthtracker.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jakedlr.healthtracker.appContainer
import com.jakedlr.healthtracker.data.FoodRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ScanResult {
    data class KnownItem(val itemId: Long) : ScanResult
    data class UnknownBarcode(val barcode: String) : ScanResult
}

class ScanViewModel(private val repository: FoodRepository) : ViewModel() {
    private val _result = MutableStateFlow<ScanResult?>(null)
    val result: StateFlow<ScanResult?> = _result.asStateFlow()

    /** [barcode] is already normalized. Only the first scan is handled. */
    fun onBarcodeScanned(barcode: String) {
        if (_result.value != null) return
        viewModelScope.launch {
            val item = repository.findByBarcode(barcode)
            _result.value = if (item != null) ScanResult.KnownItem(item.id) else ScanResult.UnknownBarcode(barcode)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { ScanViewModel(appContainer().foodRepository) }
        }
    }
}
