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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.vnsas.vnappcall.data.CallNote
import com.vnsas.vnappcall.data.SerialEntry
import com.vnsas.vnappcall.ui.theme.VNGreen

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
    var showClientPicker by remember { mutableStateOf(false) }
    var clientLoaded by remember { mutableStateOf(false) }

    // All machines belonging to the client
    val clientMachines = remember { mutableStateListOf<SerialEntry>() }
    // Which machines are selected for THIS call (tracked by index)
    val selectedIndices = remember { mutableStateListOf<Int>() }

    // Parse existing serials from note to pre-select them
    val existingSerials = remember {
        if (note != null && note.serial.isNotBlank()) {
            note.serial.split(",").map { s ->
                val parts = s.trim().split("|")
                SerialEntry(parts.getOrElse(0) { "" }, parts.getOrElse(1) { "" })
            }
        } else emptyList()
    }

    // Load client machines and pre-select matching ones
    fun loadMachinesFromClient(machinesStr: String) {
        clientMachines.clear()
        selectedIndices.clear()
        if (machinesStr.isNotBlank()) {
            val parts = machinesStr.split(",")
            parts.forEachIndexed { idx, s ->
                val fields = s.trim().split("|")
                val entry = SerialEntry(fields.getOrElse(0) { "" }, fields.getOrElse(1) { "" })
                clientMachines.add(entry)
                // Pre-select if this machine was saved in the note
                if (existingSerials.any { it.number == entry.number }) {
                    selectedIndices.add(idx)
                }
            }
            // Auto-select if client has only ONE machine and nothing was pre-selected
            if (parts.size == 1 && selectedIndices.isEmpty()) {
                selectedIndices.add(0)
            }
        }
        clientLoaded = true
    }

    // Auto-search for client by phone when dialog opens
    LaunchedEffect(phone) {
        if (phone.isNotBlank() && !clientLoaded) {
            vm.findClientByPhone(phone) { client ->
                if (client != null && client.machines.isNotBlank()) {
                    loadMachinesFromClient(client.machines)
                    if (contactName.isBlank()) contactName = client.name
                }
            }
        }
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
                text = if (note?.id == 0L || note == null) "Annota Chiamata"
                       else "Modifica Nota",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Picker buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showClientPicker = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.People, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (contactName.isBlank()) "Da Clienti"
                        else contactName,
                        maxLines = 1
                    )
                }
                OutlinedButton(
                    onClick = { showContactPicker = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Contacts, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Da Rubrica", maxLines = 1)
                }
            }

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = contactName,
                onValueChange = { contactName = it },
                label = { Text("Nome contatto") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = fieldShape,
                colors = fieldColors
            )
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
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Note") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                shape = fieldShape,
                colors = fieldColors
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = billable,
                    onCheckedChange = { billable = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary
                    )
                )
                Text("Da fatturare", modifier = Modifier.padding(end = 16.dp))
                Checkbox(
                    checked = resolved,
                    onCheckedChange = { resolved = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary
                    )
                )
                Text("Risolto")
            }

            // Client machines - SELECTABLE with checkboxes
            if (clientMachines.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = VNGreen.copy(alpha = 0.1f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Seleziona macchina/e",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = VNGreen
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Scegli quale macchina riguarda questa chiamata",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))

                        clientMachines.forEachIndexed { idx, entry ->
                            val isSelected = selectedIndices.contains(idx)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            if (!selectedIndices.contains(idx)) {
                                                selectedIndices.add(idx)
                                            }
                                        } else {
                                            selectedIndices.remove(idx)
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = VNGreen
                                    )
                                )
                                Icon(
                                    Icons.Default.Build, null,
                                    Modifier.size(16.dp),
                                    tint = if (isSelected) VNGreen
                                           else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(6.dp))
                                Column {
                                    Text(
                                        "SN: " + entry.number,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold
                                                     else FontWeight.Normal,
                                        color = if (isSelected) VNGreen
                                                else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (entry.model.isNotBlank()) {
                                        Text(
                                            entry.model,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
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
                        // Only save SELECTED machines
                        val serialStr = selectedIndices
                            .sorted()
                            .mapNotNull { idx -> clientMachines.getOrNull(idx) }
                            .filter { it.number.isNotBlank() }
                            .joinToString(",") { it.number + "|" + it.model }
                        val duration = if (note != null && note.durationSec > 0) {
                            note.durationSec
                        } else if (phone.isNotBlank()) {
                            vm.findCallDuration(
                                phone,
                                note?.timestamp ?: System.currentTimeMillis()
                            )
                        } else 0
                        onSave(
                            CallNote(
                                id = note?.id ?: 0,
                                contactName = contactName,
                                phone = phone,
                                timestamp = note?.timestamp
                                    ?: System.currentTimeMillis(),
                                note = noteText,
                                billable = billable,
                                resolved = resolved,
                                serial = serialStr,
                                durationSec = duration
                            )
                        )
                    },
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Salva") }
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
                // Auto-lookup client machines
                vm.findClientByPhone(contact.phone) { client ->
                    if (client != null && client.machines.isNotBlank()) {
                        loadMachinesFromClient(client.machines)
                    }
                }
                showContactPicker = false
            }
        )
    }

    if (showClientPicker) {
        ClientContactPickerDialog(
            vm = vm,
            onDismiss = { showClientPicker = false },
            onSelect = { client ->
                contactName = client.name
                phone = client.phone
                loadMachinesFromClient(client.machines)
                showClientPicker = false
            }
        )
    }
}
