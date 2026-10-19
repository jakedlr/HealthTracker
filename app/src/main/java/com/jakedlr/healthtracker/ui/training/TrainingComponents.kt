package com.jakedlr.healthtracker.ui.training

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.jakedlr.healthtracker.R

/** Name entry for a new or renamed category. */
@Composable
fun CategoryNameDialog(
    title: String,
    state: NameDialogState,
    onTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = state.text,
                onValueChange = onTextChange,
                label = { Text("Name") },
                singleLine = true,
                isError = state.nameTaken || state.blank,
                supportingText = {
                    when {
                        state.nameTaken -> Text("A category with this name already exists")
                        state.blank -> Text("Enter a name")
                    }
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onConfirm() }),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** A tappable row in a reorderable list, raised while it is being dragged. */
@Composable
fun ReorderableCard(
    dragging: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Card(
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = if (dragging) 8.dp else 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { content() }
            Icon(
                painterResource(R.drawable.ic_drag_handle),
                contentDescription = "Long-press to reorder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** One choice from a few options (sidedness, unit); [selected] may be null when nothing is chosen yet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> ChoiceRow(
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        options.forEachIndexed { i, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index = i, count = options.size),
                enabled = enabled,
                icon = {},
            ) { Text(label(option), maxLines = 1) }
        }
    }
}
