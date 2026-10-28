package com.example.myindiproject.data

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class ProductResponse(
    val status: Int,
    @SerializedName("status_verbose")
    val statusVerbose: String?,
    val product: OffProduct?
)

data class OffProduct(
    @SerializedName("product_name")
    val productName: String?,

    val brands: String?,
    val quantity: String?,

    @SerializedName("ingredients_text")
    val ingredients: String?,

    val allergens: String?,

    @SerializedName("allergens_tags")
    val allergensTags: List<String>?,

    val traces: String?,

    @SerializedName("traces_tags")
    val tracesTags: List<String>?,

    val nutriments: Nutriments?,

    @SerializedName("nutrition_grades")
    val nutritionGrade: String?
)

data class Nutriments(
    @SerializedName("energy-kj_100g")
    val energyKj100g: Double?,

    @SerializedName("energy-kcal_100g")
    val energyKcal100g: Double?,

    @SerializedName("fat_100g")
    val fat100g: Double?,

    @SerializedName("saturated-fat_100g")
    val saturatedFat100g: Double?,

    @SerializedName("carbohydrates_100g")
    val carbohydrates100g: Double?,

    @SerializedName("sugars_100g")
    val sugars100g: Double?,

    @SerializedName("fiber_100g")
    val fiber100g: Double?,

    @SerializedName("proteins_100g")
    val proteins100g: Double?,

    @SerializedName("salt_100g")
    val salt100g: Double?
)

interface OpenFoodFactsApi {
    @GET("api/v2/product/{barcode}.json")
    suspend fun getProduct(
        @Path("barcode") barcode: String,
        @Query("fields") fields: String =
            "product_name,brands,quantity,ingredients_text,allergens," +
                    "allergens_tags,traces,traces_tags,nutriments,nutrition_grades"
    ): ProductResponse
}

object ApiClient {
    val api: OpenFoodFactsApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://world.openfoodfacts.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenFoodFactsApi::class.java)
    }
}