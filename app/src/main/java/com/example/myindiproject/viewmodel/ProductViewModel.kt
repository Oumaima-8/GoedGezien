package com.example.myindiproject.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myindiproject.data.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProductViewModel(
    private val repository: ProductRepository = ProductRepository()
) : ViewModel() {
    private val _state = MutableStateFlow<ProductUiState>(ProductUiState.Idle)
    val state: StateFlow<ProductUiState> = _state

    fun loadProduct(barcode: String) {
        if (!barcode.matches(Regex("""\d{8}|\d{13}"""))) {
            _state.value = ProductUiState.Error("Voer een geldige barcode in.")
            return
        }

        viewModelScope.launch {
            _state.value = ProductUiState.Loading
            try {
                val product = repository.getProduct(barcode)
                _state.value = if (product == null) {
                    ProductUiState.NotFound(barcode)
                } else {
                    ProductUiState.Success(product)
                }
            } catch (_: Exception) {
                _state.value = ProductUiState.Error(
                    "Productinformatie kon niet worden opgehaald. " +
                            "Controleer uw verbinding en lees de verpakking."
                )
            }
        }
    }

    fun reset() {
        _state.value = ProductUiState.Idle
    }
}