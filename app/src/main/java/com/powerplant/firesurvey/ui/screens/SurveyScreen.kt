package com.powerplant.firesurvey.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.powerplant.firesurvey.SurveyViewModel
import com.powerplant.firesurvey.data.InspectionStatus
import com.powerplant.firesurvey.data.Issue
import com.powerplant.firesurvey.ui.theme.NotOkRed
import com.powerplant.firesurvey.ui.theme.OkGreen
import com.powerplant.firesurvey.util.Formats
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurveyScreen(vm: SurveyViewModel, code: String, onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val extinguisher by remember(code) { vm.extinguisher(code) }.collectAsStateWithLifecycle(null)
    val history by remember(code) { vm.inspections(code) }.collectAsStateWithLifecycle(emptyList())
    val savedInspector by vm.inspectorName.collectAsStateWithLifecycle()

    var status by rememberSaveable { mutableStateOf<InspectionStatus?>(null) }
    val issues = remember { mutableStateListOf<Issue>() }
    var notes by rememberSaveable { mutableStateOf("") }
    var inspector by rememberSaveable { mutableStateOf(savedInspector) }
    var saving by remember { mutableStateOf(false) }

    val needsNotes = status == InspectionStatus.NOT_OK && Issue.OTHER in issues && notes.isBlank()
    val canSave = !saving && inspector.isNotBlank() && when (status) {
        InspectionStatus.OK -> true
        InspectionStatus.NOT_OK -> issues.isNotEmpty() && !needsNotes
        null -> false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Survey") },
                navigationIcon = { IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(extinguisher?.name ?: code, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        listOfNotNull(code, extinguisher?.location?.takeIf { it.isNotBlank() }).joinToString(" · "),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val last = history.firstOrNull()
                    Text(
                        if (last == null) "Last check: never"
                        else "Last check: ${Formats.dateTime(last.timestamp)} – ${last.statusEnum.label}" +
                            if (last.issueList.isNotEmpty()) " (${last.issueList.joinToString(", ") { it.label }})" else "",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Text("Condition of the extinguisher", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatusButton(
                    text = "OK",
                    icon = { Icon(Icons.Filled.CheckCircle, null) },
                    color = OkGreen,
                    selected = status == InspectionStatus.OK,
                    onClick = { status = InspectionStatus.OK },
                    modifier = Modifier.weight(1f),
                )
                StatusButton(
                    text = "NOT OK",
                    icon = { Icon(Icons.Filled.Cancel, null) },
                    color = NotOkRed,
                    selected = status == InspectionStatus.NOT_OK,
                    onClick = { status = InspectionStatus.NOT_OK },
                    modifier = Modifier.weight(1f),
                )
            }

            if (status == InspectionStatus.NOT_OK) {
                Text("What is wrong? (select all that apply)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        Issue.entries.forEach { issue ->
                            val checked = issue in issues
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { if (checked) issues.remove(issue) else issues.add(issue) }
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(checked = checked, onCheckedChange = { if (it) issues.add(issue) else issues.remove(issue) })
                                Text(issue.label)
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (Issue.OTHER in issues && status == InspectionStatus.NOT_OK) "Notes (required for \"Other\")" else "Notes (optional)") },
                isError = needsNotes,
                minLines = 3,
            )

            OutlinedTextField(
                value = inspector,
                onValueChange = { inspector = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Inspected by") },
                isError = inspector.isBlank(),
                supportingText = { if (inspector.isBlank()) Text("Enter your name or staff ID") },
            )

            Button(
                onClick = {
                    val s = status ?: return@Button
                    saving = true
                    scope.launch {
                        vm.saveInspection(code, s, issues.toSet(), notes, inspector)
                        Toast.makeText(context, "Survey saved", Toast.LENGTH_SHORT).show()
                        onDone()
                    }
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) { Text("Save survey", style = MaterialTheme.typography.titleMedium) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatusButton(
    text: String,
    icon: @Composable () -> Unit,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val content: @Composable () -> Unit = {
        icon()
        Spacer(Modifier.size(8.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier.height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
        ) { content() }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(64.dp),
            border = BorderStroke(2.dp, color),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        ) { content() }
    }
}
