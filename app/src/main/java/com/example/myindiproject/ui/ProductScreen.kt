package com.example.myindiproject.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.myindiproject.viewmodel.Product
import java.util.Locale

@Composable
fun ProductScreen(
    product: Product,
    userAllergies: List<String>,
    onBackToScan: () -> Unit
) {
    val searchableText = listOf(
        product.ingredients,
        product.allergens,
        product.traces,
        product.allergenTags.joinToString(" "),
        product.traceTags.joinToString(" ")
    ).joinToString(" ").lowercase(Locale.ROOT)

    val matchedAllergy = userAllergies.firstOrNull { allergy ->
        allergy.isNotBlank() &&
                searchableText.contains(allergy.trim().lowercase(Locale.ROOT))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            TextButtonLabel("‹  Terug naar scannen", onBackToScan)
        }

        item {
            Text(product.name, style = MaterialTheme.typography.headlineMedium)
            if (product.brand.isNotBlank()) {
                Text(product.brand, style = MaterialTheme.typography.titleMedium)
            }
            if (product.quantity.isNotBlank()) {
                Text(product.quantity, style = MaterialTheme.typography.bodyLarge)
            }
        }

        item {
            InfoCard(
                title = when {
                    userAllergies.isEmpty() -> "Allergieën niet ingesteld"
                    matchedAllergy != null -> "Let op: allergie"
                    searchableText.isBlank() -> "Allergiecontrole onzeker"
                    else -> "Controleer de verpakking"
                },
                body = when {
                    userAllergies.isEmpty() ->
                        "Voeg allergieën toe aan uw profiel om productinformatie te vergelijken."
                    matchedAllergy != null ->
                        "“$matchedAllergy” komt voor in de beschikbare informatie. " +
                                "Eet dit product niet als u hiervoor allergisch bent."
                    searchableText.isBlank() ->
                        "Er is onvoldoende allergeneninformatie. Lees de verpakking."
                    else ->
                        "Geen overeenkomst gevonden in de beschikbare gegevens. " +
                                "Dat betekent niet dat het product veilig is. Lees de verpakking."
                },
                warning = matchedAllergy != null || searchableText.isBlank()
            )
        }

        item {
            InfoCard(
                title = "Medicijncontrole onzeker",
                body = "Een betrouwbare controle op invloed op medicijnen is niet " +
                        "beschikbaar. Vraag uw apotheker om advies. Verander uw medicijnen niet zelf.",
                warning = true
            )
        }

        item { InfoCard("Ingrediënten", product.ingredients.ifBlank { "Niet beschikbaar." }) }
        item { InfoCard("Allergenen", product.allergens.ifBlank { "Niet vermeld." }) }

        if (product.traces.isNotBlank()) {
            item { InfoCard("Kan sporen bevatten", product.traces) }
        }

        item {
            InfoCard(
                "Voedingswaarden per 100 gram",
                if (product.nutrition.isEmpty()) {
                    "Voedingswaarden zijn niet beschikbaar."
                } else {
                    product.nutrition.joinToString("\n") { (label, value) ->
                        "$label: $value"
                    }
                }
            )
        }

        if (product.nutritionGrade.isNotBlank()) {
            item {
                InfoCard(
                    "Nutri-Score ${product.nutritionGrade}",
                    "De score zegt niets over allergieën of medicijnen."
                )
            }
        }

        item {
            Text(
                "Productinformatie kan ontbreken of onjuist zijn. Lees altijd de verpakking.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    body: String,
    warning: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = if (warning) Color(0xFF9E2A20) else Color(0xFF18324B)
            )
            Text(
                body,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun TextButtonLabel(text: String, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}