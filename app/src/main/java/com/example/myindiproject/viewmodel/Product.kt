package com.example.myindiproject.viewmodel

data class Product(
    val barcode: String,
    val name: String,
    val brand: String,
    val quantity: String,
    val ingredients: String,
    val allergens: String,
    val allergenTags: List<String>,
    val traces: String,
    val traceTags: List<String>,
    val nutrition: List<Pair<String, String>>,
    val nutritionGrade: String
)

sealed interface ProductUiState {
    data object Idle : ProductUiState
    data object Loading : ProductUiState
    data class Success(val product: Product) : ProductUiState
    data class NotFound(val barcode: String) : ProductUiState
    data class Error(val message: String) : ProductUiState
}