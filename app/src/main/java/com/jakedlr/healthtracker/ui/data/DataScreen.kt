package com.jakedlr.healthtracker.ui.data

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jakedlr.healthtracker.MainActivity
import com.jakedlr.healthtracker.data.DataSummary
import com.jakedlr.healthtracker.ui.BackButton
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataScreen(
    onBack: () -> Unit,
    viewModel: DataViewModel = viewModel(factory = DataViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Restart the screen stack so every screen loads from the imported database.
    LaunchedEffect(state.importFinished) {
        if (state.importFinished) {
            val activity = context.findActivity() ?: return@LaunchedEffect
            activity.startActivity(
                Intent(activity, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
            )
            activity.finish()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri -> uri?.let(viewModel::export) }
    // .db files have no reliable MIME type in Downloads, so allow any file and check it ourselves.
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::inspect)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Data") }, navigationIcon = { BackButton(onBack) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("On this phone", style = MaterialTheme.typography.labelLarge)
                    Text(state.current?.let(::describeWithSpan) ?: "…", style = MaterialTheme.typography.bodyLarge)
                }
            }

            Text(
                "Export saves a copy of all your data to a file you choose (for example in Downloads). " +
                    "Import replaces everything on this phone with the contents of an exported file.",
                style = MaterialTheme.typography.bodyMedium,
            )

            val busy = state.task != null
            Button(
                onClick = { exportLauncher.launch(viewModel.suggestedFileName()) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Export data") }
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("*/*")) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Import data") }

            state.task?.let { task ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(
                        when (task) {
                            DataTask.EXPORTING -> "Exporting…"
                            DataTask.CHECKING -> "Checking file…"
                            DataTask.IMPORTING -> "Importing…"
                        },
                    )
                }
            }

            state.message?.let { message ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(message, style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = viewModel::dismissMessage, modifier = Modifier.align(Alignment.End)) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }

    state.pendingImport?.let { pending ->
        AlertDialog(
            onDismissRequest = viewModel::cancelImport,
            title = { Text("Replace all data?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("File: ${describeWithSpan(pending.summary)}")
                    state.current?.let { Text("On this phone now: ${describeWithSpan(it)}") }
                    Text(
                        "Everything currently on this phone will be replaced by the file's contents.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmImport,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Replace") }
            },
            dismissButton = { TextButton(onClick = viewModel::cancelImport) { Text("Cancel") } },
        )
    }
}

private fun describeWithSpan(s: DataSummary): String {
    val fmt = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    val span = if (s.firstDay != null && s.lastDay != null) " (${s.firstDay.format(fmt)} – ${s.lastDay.format(fmt)})" else ""
    return describe(s) + span
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
