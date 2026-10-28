package com.example.myindiproject.data

import com.example.myindiproject.viewmodel.Product

class ProductRepository(
    private val api: OpenFoodFactsApi = ApiClient.api
) {
    suspend fun getProduct(barcode: String): Product? {
        val response = api.getProduct(barcode)

        // In the v2 response, status 1 means a product was found.
        if (response.status != 1) return null

        val item = response.product ?: return null
        val name = item.productName?.trim().orEmpty()
        if (name.isBlank()) return null

        val values = item.nutriments
        val nutrition = listOfNotNull(
            values?.energyKj100g?.let { "Energie" to "$it kJ" },
            values?.energyKcal100g?.let { "Energie" to "$it kcal" },
            values?.fat100g?.let { "Vetten" to "$it g" },
            values?.saturatedFat100g?.let { "Verzadigd vet" to "$it g" },
            values?.carbohydrates100g?.let { "Koolhydraten" to "$it g" },
            values?.sugars100g?.let { "Suikers" to "$it g" },
            values?.fiber100g?.let { "Vezels" to "$it g" },
            values?.proteins100g?.let { "Eiwitten" to "$it g" },
            values?.salt100g?.let { "Zout" to "$it g" }
        )

        return Product(
            barcode = barcode,
            name = name,
            brand = item.brands.orEmpty(),
            quantity = item.quantity.orEmpty(),
            ingredients = item.ingredients.orEmpty(),
            allergens = item.allergens.orEmpty(),
            allergenTags = item.allergensTags.orEmpty(),
            traces = item.traces.orEmpty(),
            traceTags = item.tracesTags.orEmpty(),
            nutrition = nutrition,
            nutritionGrade = item.nutritionGrade.orEmpty().uppercase()
        )
    }
}