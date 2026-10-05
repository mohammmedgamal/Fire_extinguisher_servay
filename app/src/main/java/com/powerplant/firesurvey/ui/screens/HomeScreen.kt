package com.powerplant.firesurvey.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.powerplant.firesurvey.SurveyViewModel
import com.powerplant.firesurvey.data.ExtinguisherSummary
import com.powerplant.firesurvey.data.InspectionStatus
import com.powerplant.firesurvey.util.Formats
import kotlinx.coroutines.launch

private enum class Filter(val label: String) { ALL("All"), NOT_OK("Not OK"), DUE("Due / never") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: SurveyViewModel, onScan: () -> Unit, onOpen: (String) -> Unit, onAdd: () -> Unit) {
    val summaries by vm.summaries.collectAsStateWithLifecycle()
    val inspector by vm.inspectorName.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(Filter.ALL) }
    var showNameDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Fire Extinguisher Survey", fontWeight = FontWeight.Bold)
                        Text(
                            if (inspector.isBlank()) "Tap the person icon to set your name" else "Operator: $inspector",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                actions = {
                    IconButton(onClick = { showNameDialog = true }) { Icon(Icons.Filled.Person, "Set operator name") }
                    IconButton(onClick = { scope.launch { vm.exportCsv(context) } }) {
                        Icon(Icons.Filled.FileDownload, "Export survey report (CSV)")
                    }
                    IconButton(onClick = onAdd) { Icon(Icons.Filled.Add, "Register extinguisher") }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onScan,
                icon = { Icon(Icons.Filled.QrCodeScanner, null) },
                text = { Text("Scan QR code") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { padding ->
        val list = summaries
        if (list == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }

        val overdueCount = list.count { it.lastStatusEnum != InspectionStatus.NOT_OK && Formats.isOverdue(it.lastTimestamp) }
        val notOkCount = list.count { it.lastStatusEnum == InspectionStatus.NOT_OK }
        val filtered = list.filter { s ->
            val e = s.extinguisher
            val matchesQuery = query.isBlank() || listOf(e.id, e.name, e.location, e.type).any { it.contains(query.trim(), ignoreCase = true) }
            val matchesFilter = when (filter) {
                Filter.ALL -> true
                Filter.NOT_OK -> s.lastStatusEnum == InspectionStatus.NOT_OK
                Filter.DUE -> Formats.isOverdue(s.lastTimestamp)
            }
            matchesQuery && matchesFilter
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("Total", list.size.toString(), Modifier.weight(1f))
                    StatCard("Not OK", notOkCount.toString(), Modifier.weight(1f))
                    StatCard("Due", overdueCount.toString(), Modifier.weight(1f))
                }
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    placeholder = { Text("Search name, code or location") },
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Filter.entries.forEach { f ->
                        FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f.label) })
                    }
                }
            }
            if (list.isEmpty()) {
                item {
                    Text(
                        "No extinguishers registered yet.\n\nScan an extinguisher's QR code to register it, " +
                            "or tap + to add one and print its QR label.",
                        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else if (filtered.isEmpty()) {
                item {
                    Text(
                        "No matches",
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(filtered, key = { it.extinguisher.id }) { summary ->
                ExtinguisherRow(summary, onClick = { onOpen(summary.extinguisher.id) })
            }
        }
    }

    if (showNameDialog) {
        InspectorNameDialog(
            current = inspector,
            onDismiss = { showNameDialog = false },
            onSave = { vm.setInspectorName(it); showNameDialog = false },
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun ExtinguisherRow(summary: ExtinguisherSummary, onClick: () -> Unit) {
    val e = summary.extinguisher
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(e.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOf(e.id, e.location).filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                StatusBadge(summary.lastStatusEnum, overdue = Formats.isOverdue(summary.lastTimestamp))
            }
            Spacer(Modifier.height(6.dp))
            val last = summary.lastTimestamp
            Text(
                if (last == null) "Last check: never" else "Last check: ${Formats.date(last)} (${Formats.relative(last)})",
                style = MaterialTheme.typography.bodySmall,
            )
            if (summary.lastStatusEnum == InspectionStatus.NOT_OK && summary.lastIssueList.isNotEmpty()) {
                Text(
                    summary.lastIssueList.joinToString(", ") { it.label },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
