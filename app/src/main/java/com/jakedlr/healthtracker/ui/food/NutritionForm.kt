package com.jakedlr.healthtracker.ui.food

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jakedlr.healthtracker.domain.Nutrition
import com.jakedlr.healthtracker.domain.parseNutrient
import com.jakedlr.healthtracker.ui.formatAmount
import com.jakedlr.healthtracker.ui.formatGrams
import com.jakedlr.healthtracker.ui.formatKcal

/** Raw text for the five per-serving nutrition inputs. */
data class NutritionInput(
    val calories: String = "",
    val protein: String = "",
    val carbs: String = "",
    val fiber: String = "",
    val fat: String = "",
) {
    /** Null if any field is missing or invalid. */
    fun parse(): Nutrition? {
        return Nutrition(
            caloriesKcal = parseNutrient(calories) ?: return null,
            proteinG = parseNutrient(protein) ?: return null,
            carbsG = parseNutrient(carbs) ?: return null,
            fiberG = parseNutrient(fiber) ?: return null,
            fatG = parseNutrient(fat) ?: return null,
        )
    }

    companion object {
        fun from(n: Nutrition) = NutritionInput(
            calories = formatAmount(n.caloriesKcal),
            protein = formatAmount(n.proteinG),
            carbs = formatAmount(n.carbsG),
            fiber = formatAmount(n.fiberG),
            fat = formatAmount(n.fatG),
        )
    }
}

/** Five per-serving inputs. When [showErrors] is set, each invalid field is highlighted. */
@Composable
fun NutritionFields(
    input: NutritionInput,
    onChange: (NutritionInput) -> Unit,
    showErrors: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Per serving", style = MaterialTheme.typography.labelLarge)
        NutrientField("Calories (kcal)", input.calories, showErrors, Modifier.fillMaxWidth()) {
            onChange(input.copy(calories = it))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NutrientField("Protein (g)", input.protein, showErrors, Modifier.weight(1f)) {
                onChange(input.copy(protein = it))
            }
            NutrientField("Carbs (g)", input.carbs, showErrors, Modifier.weight(1f)) {
                onChange(input.copy(carbs = it))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NutrientField("Fat (g)", input.fat, showErrors, Modifier.weight(1f)) {
                onChange(input.copy(fat = it))
            }
            NutrientField("Fiber (g)", input.fiber, showErrors, Modifier.weight(1f)) {
                onChange(input.copy(fiber = it))
            }
        }
    }
}

@Composable
private fun NutrientField(
    label: String,
    value: String,
    showErrors: Boolean,
    modifier: Modifier,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = showErrors && parseNutrient(value) == null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
        modifier = modifier,
    )
}

/** e.g. "250 kcal · P 12.0 g · C 30.0 g · F 8.0 g · Fiber 4.0 g". */
fun nutritionSummary(n: Nutrition): String =
    "${formatKcal(n.caloriesKcal)} kcal · P ${formatGrams(n.proteinG)} g · " +
        "C ${formatGrams(n.carbsG)} g · F ${formatGrams(n.fatG)} g · Fiber ${formatGrams(n.fiberG)} g"
