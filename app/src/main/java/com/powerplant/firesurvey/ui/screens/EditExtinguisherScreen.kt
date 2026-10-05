package com.powerplant.firesurvey.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.powerplant.firesurvey.SurveyViewModel
import com.powerplant.firesurvey.data.Extinguisher
import com.powerplant.firesurvey.data.ExtinguisherTypes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExtinguisherScreen(
    vm: SurveyViewModel,
    initialCode: String,
    isNew: Boolean,
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    onDeleted: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var original by remember { mutableStateOf<Extinguisher?>(null) }
    var code by rememberSaveable { mutableStateOf(initialCode) }
    var name by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(ExtinguisherTypes.first()) }
    var capacity by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var loadedOnce by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(initialCode, isNew) {
        if (!isNew) {
            val e = vm.getExtinguisher(initialCode)
            original = e
            if (e != null && !loadedOnce) {
                name = e.name; location = e.location; type = e.type; capacity = e.capacity
                loadedOnce = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "Register extinguisher" else "Edit extinguisher") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    if (!isNew && original != null) {
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, "Delete") }
                    }
                },
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                enabled = isNew,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("QR code / ID *") },
                supportingText = {
                    Text(if (isNew) "Text encoded in the QR label, e.g. FE-TURB-001" else "The QR code cannot be changed")
                },
            )
            OutlinedTextField(
                value = name, onValueChange = { name = it }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), label = { Text("Name *") },
                placeholder = { Text("e.g. Turbine hall – extinguisher 3") },
            )
            OutlinedTextField(
                value = location, onValueChange = { location = it }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), label = { Text("Location") },
                placeholder = { Text("e.g. Unit 2, Level 0, near switchgear") },
            )
            TypeDropdown(type, onSelect = { type = it })
            OutlinedTextField(
                value = capacity, onValueChange = { capacity = it }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), label = { Text("Capacity") },
                placeholder = { Text("e.g. 6 kg") },
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = {
                    scope.launch {
                        val e = Extinguisher(
                            id = code.trim(),
                            name = name.trim(),
                            location = location.trim(),
                            type = type,
                            capacity = capacity.trim(),
                            createdAt = original?.createdAt ?: System.currentTimeMillis(),
                        )
                        error = vm.saveExtinguisher(e, isNew)
                        if (error == null) onSaved(e.id)
                    }
                },
                enabled = code.isNotBlank() && name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Save") }
            if (isNew) {
                Text(
                    "After saving, open the extinguisher and tap the QR icon to share or print its label.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete extinguisher?") },
            text = { Text("This also deletes its whole inspection history. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    original?.let { e -> scope.launch { vm.deleteExtinguisher(e); onDeleted() } }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypeDropdown(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ExtinguisherTypes.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false })
            }
        }
    }
}
