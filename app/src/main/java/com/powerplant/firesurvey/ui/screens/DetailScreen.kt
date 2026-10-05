package com.powerplant.firesurvey.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.powerplant.firesurvey.SurveyViewModel
import com.powerplant.firesurvey.data.Extinguisher
import com.powerplant.firesurvey.data.Inspection
import com.powerplant.firesurvey.data.InspectionStatus
import com.powerplant.firesurvey.ui.theme.NotOkRed
import com.powerplant.firesurvey.ui.theme.OkGreen
import com.powerplant.firesurvey.util.Formats
import com.powerplant.firesurvey.util.QrGenerator
import com.powerplant.firesurvey.util.Sharing
import kotlinx.coroutines.flow.map

/** Wrapper so the UI can tell "still loading" apart from "no such extinguisher". */
private class Loaded<T>(val value: T)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    vm: SurveyViewModel,
    code: String,
    onBack: () -> Unit,
    onSurvey: () -> Unit,
    onEdit: () -> Unit,
    onRegister: () -> Unit,
) {
    val loaded by remember(code) { vm.extinguisher(code).map { Loaded(it) } }.collectAsStateWithLifecycle(null)
    val history by remember(code) { vm.inspections(code) }.collectAsStateWithLifecycle(emptyList())
    var showQr by remember { mutableStateOf(false) }
    val extinguisher = loaded?.value

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(extinguisher?.name ?: "Extinguisher") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    if (extinguisher != null) {
                        IconButton(onClick = { showQr = true }) { Icon(Icons.Filled.QrCode2, "Show QR label") }
                        IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, "Edit") }
                    }
                },
            )
        },
    ) { padding ->
        when {
            loaded == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            extinguisher == null -> UnknownCode(code, onRegister, onBack, Modifier.padding(padding))
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { InfoCard(extinguisher) }
                item { LastCheckCard(history.firstOrNull()) }
                item {
                    Button(onClick = onSurvey, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                        Icon(Icons.Filled.FactCheck, null)
                        Spacer(Modifier.size(8.dp))
                        Text("Start survey", style = MaterialTheme.typography.titleMedium)
                    }
                }
                if (history.size > 1) {
                    item { Text("Inspection history", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
                    items(history.drop(1), key = { it.id }) { HistoryRow(it) }
                }
            }
        }
    }

    if (showQr && extinguisher != null) {
        QrLabelDialog(extinguisher, onDismiss = { showQr = false })
    }
}

@Composable
private fun UnknownCode(code: String, onRegister: () -> Unit, onBack: () -> Unit, modifier: Modifier) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(56.dp))
        Text("Unknown extinguisher", style = MaterialTheme.typography.headlineSmall)
        Text("No extinguisher is registered with code:\n\"$code\"", textAlign = TextAlign.Center)
        Button(onClick = onRegister, modifier = Modifier.fillMaxWidth()) { Text("Register this extinguisher") }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}

@Composable
private fun InfoCard(e: Extinguisher) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(e.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row {
                LabeledValue("Code", e.id, Modifier.weight(1f))
                LabeledValue("Location", e.location, Modifier.weight(1f))
            }
            Row {
                LabeledValue("Type", e.type, Modifier.weight(1f))
                LabeledValue("Capacity", e.capacity, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LastCheckCard(last: Inspection?) {
    val status = last?.statusEnum
    val container = when (status) {
        InspectionStatus.OK -> OkGreen.copy(alpha = 0.10f)
        InspectionStatus.NOT_OK -> NotOkRed.copy(alpha = 0.10f)
        null -> MaterialTheme.colorScheme.surfaceVariant
    }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = container)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Last check", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                StatusBadge(status, overdue = Formats.isOverdue(last?.timestamp))
            }
            if (last == null) {
                Text("This extinguisher has never been surveyed.")
            } else {
                Row {
                    LabeledValue("Date", "${Formats.dateTime(last.timestamp)}\n(${Formats.relative(last.timestamp)})", Modifier.weight(1f))
                    LabeledValue("Checked by", last.inspector, Modifier.weight(1f))
                }
                LabeledValue(
                    "Condition",
                    if (status == InspectionStatus.OK) "OK – no defects found" else last.issueList.joinToString("\n") { "• ${it.label}" },
                    valueColor = if (status == InspectionStatus.OK) OkGreen else NotOkRed,
                )
                if (last.notes.isNotBlank()) LabeledValue("Notes", last.notes)
                if (Formats.isOverdue(last.timestamp)) {
                    Text(
                        "Monthly check is due (last check over ${Formats.INSPECTION_INTERVAL_DAYS} days ago).",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(i: Inspection) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(Formats.dateTime(i.timestamp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(i.inspector.ifBlank { "—" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StatusBadge(i.statusEnum)
        }
        if (i.issueList.isNotEmpty()) {
            Text(i.issueList.joinToString(", ") { it.label }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        if (i.notes.isNotBlank()) Text(i.notes, style = MaterialTheme.typography.bodySmall)
        HorizontalDivider(Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun QrLabelDialog(e: Extinguisher, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val bitmap = remember(e.id) { QrGenerator.qrBitmap(e.id, 600).asImageBitmap() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("QR label") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Image(bitmap, contentDescription = "QR code for ${e.id}", modifier = Modifier.size(240.dp))
                Text(e.name, fontWeight = FontWeight.Bold)
                Text(e.id)
            }
        },
        confirmButton = { TextButton(onClick = { Sharing.shareLabel(context, e) }) { Text("Share / print") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
