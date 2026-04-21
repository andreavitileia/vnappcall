package com.vnsas.vnappcall.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vnsas.vnappcall.MainViewModel
import com.vnsas.vnappcall.data.ClientContact
import com.vnsas.vnappcall.data.SerialEntry
import com.vnsas.vnappcall.ui.theme.VNGreen

val MACHINE_TYPES = listOf("SM225", "EUROSPEEDY", "SMM", "SM3000", "SM7000", "SMONE")

val StatusGreen = Color(0xFF4CAF50)
val StatusYellow = Color(0xFFFFC107)
val StatusRed = Color(0xFFF44336)

fun statusColor(flag: Int): Color = when (flag) {
    0 -> StatusGreen
    1 -> StatusYellow
    2 -> StatusRed
    else -> StatusGreen
}

fun statusLabel(flag: Int): String = when (flag) {
    0 -> "Buon cliente"
    1 -> "Problemi passati"
    2 -> "Problemi attuali"
    else -> "Buon cliente"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditClientContactDialog(
    contact: ClientContact?,
    prefillName: String = "",
    prefillPhone: String = "",
    prefillMachines: String = "",
    vm: MainViewModel,
    onDismiss: () -> Unit,
    onSave: (ClientContact) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(contact?.name ?: prefillName) }
    var phone by remember { mutableStateOf(contact?.phone ?: prefillPhone) }
    var statusFlag by remember { mutableIntStateOf(contact?.statusFlag ?: 0) }

    val machines = remember {
        val list = mutableStateListOf<SerialEntry>()
        val src = contact?.machines ?: prefillMachines
        if (src.isNotBlank()) {
            src.split(",").forEach { s ->
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
                text = if (contact == null) "Gestisci Macchine" else "Modifica Macchine",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (phone.isNotBlank()) {
                Text(
                    text = phone,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(16.dp))

            // --- SEMAPHORE STATUS ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = statusColor(statusFlag).copy(alpha = 0.08f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "Stato Cliente",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor(statusFlag)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Seleziona lo stato del cliente",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Green button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { statusFlag = 0 }
                                .then(
                                    if (statusFlag == 0) Modifier
                                        .background(StatusGreen.copy(alpha = 0.15f))
                                        .border(2.dp, StatusGreen, RoundedCornerShape(12.dp))
                                    else Modifier
                                        .background(Color.Transparent)
                                )
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(StatusGreen)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Buono",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (statusFlag == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (statusFlag == 0) StatusGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Yellow button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { statusFlag = 1 }
                                .then(
                                    if (statusFlag == 1) Modifier
                                        .background(StatusYellow.copy(alpha = 0.15f))
                                        .border(2.dp, StatusYellow, RoundedCornerShape(12.dp))
                                    else Modifier
                                        .background(Color.Transparent)
                                )
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(StatusYellow)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Attenzione",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (statusFlag == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (statusFlag == 1) StatusYellow else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Red button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { statusFlag = 2 }
                                .then(
                                    if (statusFlag == 2) Modifier
                                        .background(StatusRed.copy(alpha = 0.15f))
                                        .border(2.dp, StatusRed, RoundedCornerShape(12.dp))
                                    else Modifier
                                        .background(Color.Transparent)
                                )
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(StatusRed)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Problema",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (statusFlag == 2) FontWeight.Bold else FontWeight.Normal,
                                color = if (statusFlag == 2) StatusRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    Text(
                        statusLabel(statusFlag),
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor(statusFlag),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

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
                        "Seleziona il tipo di macchina dal menu e inserisci il seriale",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(12.dp))

                    machines.forEachIndexed { idx, entry ->
                        var modelExpanded by remember { mutableStateOf(false) }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Macchina " + (idx + 1).toString(),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = VNGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = { machines.removeAt(idx) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            "Rimuovi",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))

                                // Dropdown for machine type
                                Box {
                                    OutlinedTextField(
                                        value = entry.model,
                                        onValueChange = { newVal ->
                                            machines[idx] = entry.copy(model = newVal)
                                        },
                                        label = { Text("Tipo macchina") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = fieldShape,
                                        colors = fieldColors,
                                        trailingIcon = {
                                            IconButton(onClick = { modelExpanded = !modelExpanded }) {
                                                Icon(Icons.Default.ArrowDropDown, "Seleziona tipo")
                                            }
                                        }
                                    )
                                    DropdownMenu(
                                        expanded = modelExpanded,
                                        onDismissRequest = { modelExpanded = false }
                                    ) {
                                        MACHINE_TYPES.forEach { machineType ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        machineType,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                },
                                                onClick = {
                                                    machines[idx] = entry.copy(model = machineType)
                                                    modelExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(10.dp))

                                // Serial number field
                                OutlinedTextField(
                                    value = entry.number,
                                    onValueChange = { newVal ->
                                        machines[idx] = entry.copy(number = newVal)
                                    },
                                    label = { Text("Numero seriale (SN)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = fieldShape,
                                    colors = fieldColors
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Annulla") }
                Button(
                    onClick = {
                        val machinesStr = machines
                            .filter { it.number.isNotBlank() || it.model.isNotBlank() }
                            .joinToString(",") { it.number + "|" + it.model }
                        onSave(
                            ClientContact(
                                id = contact?.id ?: 0,
                                name = name,
                                phone = phone,
                                machines = machinesStr,
                                statusFlag = statusFlag
                            )
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Salva") }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
