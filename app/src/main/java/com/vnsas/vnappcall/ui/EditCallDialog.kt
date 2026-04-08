package com.vnsas.vnappcall.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vnsas.vnappcall.MainViewModel
import com.vnsas.vnappcall.data.CallNote
import com.vnsas.vnappcall.data.SerialEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCallDialog(
    note: CallNote?,
    vm: MainViewModel,
    onDismiss: () -> Unit,
    onSave: (CallNote) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var contactName by remember { mutableStateOf(note?.contactName ?: "") }
    var phone by remember { mutableStateOf(note?.phone ?: "") }
    var noteText by remember { mutableStateOf(note?.note ?: "") }
    var billable by remember { mutableStateOf(note?.billable ?: false) }
    var resolved by remember { mutableStateOf(note?.resolved ?: false) }
    var showContactPicker by remember { mutableStateOf(false) }

    // Parse existing serials
    val serials = remember {
        val list = mutableStateListOf<SerialEntry>()
        if (note != null && note.serial.isNotBlank()) {
            note.serial.split(",").forEach { s ->
                val parts = s.trim().split("|")
                list.add(SerialEntry(parts.getOrElse(0) { "" }, parts.getOrElse(1) { "" }))
            }
        }
        list
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = if (note == null) "Nuova Chiamata" else "Modifica Chiamata",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Contact name + picker
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = contactName,
                    onValueChange = { contactName = it },
                    label = { Text("Nome contatto") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                IconButton(onClick = { showContactPicker = true }) {
                    Icon(Icons.Default.People, "Scegli contatto")
                }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Telefono") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Note") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = billable, onCheckedChange = { billable = it })
                Text("Da fatturare", modifier = Modifier.padding(end = 16.dp))
                Checkbox(checked = resolved, onCheckedChange = { resolved = it })
                Text("Risolto")
            }

            // Serials section
            Spacer(Modifier.height(12.dp))
            Text(
                "Seriali macchine",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            serials.forEachIndexed { idx, entry ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = entry.number,
                        onValueChange = { serials[idx] = entry.copy(number = it) },
                        label = { Text("Seriale") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = entry.model,
                        onValueChange = { serials[idx] = entry.copy(model = it) },
                        label = { Text("Modello") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    IconButton(onClick = { serials.removeAt(idx) }) {
                        Icon(Icons.Default.Close, "Rimuovi")
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            TextButton(onClick = { serials.add(SerialEntry()) }) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(4.dp))
                Text("Aggiungi seriale")
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = onDismiss) {
                    Text("Annulla")
                }
                Spacer(Modifier.width(12.dp))
                Button(onClick = {
                    val serialStr = serials
                        .filter { it.number.isNotBlank() }
                        .joinToString(",") { "${it.number}|${it.model}" }

                    val duration = if (note?.durationSec != null && note.durationSec > 0) {
                        note.durationSec
                    } else if (phone.isNotBlank()) {
                        vm.findCallDuration(phone, note?.timestamp ?: System.currentTimeMillis())
                    } else 0

                    onSave(
                        CallNote(
                            id = note?.id ?: 0,
                            contactName = contactName,
                            phone = phone,
                            timestamp = note?.timestamp ?: System.currentTimeMillis(),
                            note = noteText,
                            billable = billable,
                            resolved = resolved,
                            serial = serialStr,
                            durationSec = duration
                        )
                    )
                }) {
                    Text("Salva")
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showContactPicker) {
        ContactPickerDialog(
            vm = vm,
            onDismiss = { showContactPicker = false },
            onSelect = { contact ->
                contactName = contact.name
                phone = contact.phone
                showContactPicker = false
            }
        )
    }
}
