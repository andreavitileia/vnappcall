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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vnsas.vnappcall.MainViewModel
import com.vnsas.vnappcall.data.ClientContact
import com.vnsas.vnappcall.data.SerialEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditClientContactDialog(
    contact: ClientContact?,
    vm: MainViewModel,
    onDismiss: () -> Unit,
    onSave: (ClientContact) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(contact?.name ?: "") }
    var phone by remember { mutableStateOf(contact?.phone ?: "") }
    var showPhonePicker by remember { mutableStateOf(false) }

    val machines = remember {
        val list = mutableStateListOf<SerialEntry>()
        if (contact != null && contact.machines.isNotBlank()) {
            contact.machines.split(",").forEach { s ->
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
                text = if (contact == null) "Nuovo Cliente" else "Modifica Cliente",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Name + phone picker
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome cliente") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                IconButton(onClick = { showPhonePicker = true }) {
                    Icon(Icons.Default.People, "Da rubrica")
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

            // Machines section
            Spacer(Modifier.height(16.dp))
            Text(
                "Macchine e Seriali",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            machines.forEachIndexed { idx, entry ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = entry.number,
                        onValueChange = { machines[idx] = entry.copy(number = it) },
                        label = { Text("Seriale") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = entry.model,
                        onValueChange = { machines[idx] = entry.copy(model = it) },
                        label = { Text("Modello") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    IconButton(onClick = { machines.removeAt(idx) }) {
                        Icon(Icons.Default.Close, "Rimuovi")
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            TextButton(onClick = { machines.add(SerialEntry()) }) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(4.dp))
                Text("Aggiungi macchina")
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = onDismiss) {
                    Text("Annulla")
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = {
                        val machinesStr = machines
                            .filter { it.number.isNotBlank() || it.model.isNotBlank() }
                            .joinToString(",") { "${it.number}|${it.model}" }
                        onSave(
                            ClientContact(
                                id = contact?.id ?: 0,
                                name = name,
                                phone = phone,
                                machines = machinesStr
                            )
                        )
                    },
                    enabled = name.isNotBlank()
                ) {
                    Text("Salva")
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showPhonePicker) {
        ContactPickerDialog(
            vm = vm,
            onDismiss = { showPhonePicker = false },
            onSelect = { c ->
                name = c.name
                phone = c.phone
                showPhonePicker = false
            }
        )
    }
}
