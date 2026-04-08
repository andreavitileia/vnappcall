package com.vnsas.vnappcall.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import com.vnsas.vnappcall.ui.theme.VNGreen

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

    val fieldShape = RoundedCornerShape(12.dp)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
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

            // Name + rubrica picker
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome cliente") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = fieldShape,
                    colors = fieldColors
                )
                IconButton(onClick = { showPhonePicker = true }) {
                    Icon(
                        Icons.Default.Contacts,
                        "Da rubrica",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Telefono") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = fieldShape,
                colors = fieldColors
            )

            // ===== MACHINES SECTION =====
            Spacer(Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = VNGreen.copy(alpha = 0.08f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "Macchine e Seriali",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = VNGreen
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Aggiungi le macchine del cliente con numero seriale e modello",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(12.dp))

                    // Existing machines
                    machines.forEachIndexed { idx, entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = entry.number,
                                onValueChange = { newVal ->
                                    machines[idx] = entry.copy(number = newVal)
                                },
                                label = { Text("Seriale") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = fieldShape,
                                colors = fieldColors
                            )
                            Spacer(Modifier.width(8.dp))
                            OutlinedTextField(
                                value = entry.model,
                                onValueChange = { newVal ->
                                    machines[idx] = entry.copy(model = newVal)
                                },
                                label = { Text("Modello") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = fieldShape,
                                colors = fieldColors
                            )
                            IconButton(onClick = { machines.removeAt(idx) }) {
                                Icon(
                                    Icons.Default.Close,
                                    "Rimuovi",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    // Add machine button - prominent
                    Button(
                        onClick = { machines.add(SerialEntry()) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VNGreen,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Aggiungi macchina")
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Save / Cancel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Annulla") }
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
                    enabled = name.isNotBlank(),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Salva") }
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
