package com.vnsas.vnappcall.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vnsas.vnappcall.MainViewModel
import com.vnsas.vnappcall.data.ClientContact
import com.vnsas.vnappcall.data.SerialEntry
import com.vnsas.vnappcall.ui.theme.VNGreen
import com.vnsas.vnappcall.util.PhoneContact

private data class MergedContact(
    val name: String,
    val phone: String,
    val clientContact: ClientContact?,
    val machineCount: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsTab(vm: MainViewModel) {
    val phoneContacts by vm.contacts.collectAsState()
    val clientContacts by vm.clientContacts.collectAsState()
    var query by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var editContact by remember { mutableStateOf<ClientContact?>(null) }
    var editPhoneContact by remember { mutableStateOf<PhoneContact?>(null) }

    LaunchedEffect(Unit) {
        vm.refreshContacts()
    }

    val mergedContacts = remember(phoneContacts, clientContacts) {
        try {
            val clientByPhone = mutableMapOf<String, ClientContact>()
            clientContacts.forEach { client ->
                val key = client.phone.replace(Regex("[^0-9+]"), "").takeLast(7)
                if (key.length >= 4) clientByPhone[key] = client
            }

            val usedClientIds = mutableSetOf<Long>()
            val merged = mutableListOf<MergedContact>()

            phoneContacts.forEach { pc ->
                try {
                    val phoneKey = pc.phone.replace(Regex("[^0-9+]"), "").takeLast(7)
                    val client = if (phoneKey.length >= 4) clientByPhone[phoneKey] else null
                    if (client != null) usedClientIds.add(client.id)
                    val machines = if (client != null && client.machines.isNotBlank()) {
                        client.machines.split(",").size
                    } else 0
                    merged.add(MergedContact(
                        name = pc.name,
                        phone = pc.phone,
                        clientContact = client,
                        machineCount = machines
                    ))
                } catch (_: Throwable) {
                    // Skip problematic contact
                }
            }

            clientContacts.filter { it.id !in usedClientIds }.forEach { client ->
                try {
                    val machines = if (client.machines.isNotBlank()) {
                        client.machines.split(",").size
                    } else 0
                    merged.add(MergedContact(
                        name = client.name,
                        phone = client.phone,
                        clientContact = client,
                        machineCount = machines
                    ))
                } catch (_: Throwable) {
                    // Skip problematic client contact
                }
            }

            merged.sortedWith(compareByDescending<MergedContact> { it.machineCount > 0 }
                .thenBy { it.name.lowercase() })
        } catch (_: Throwable) {
            emptyList()
        }
    }

    val filteredContacts = remember(mergedContacts, query) {
        if (query.isBlank()) mergedContacts
        else mergedContacts.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.phone.contains(query) ||
            (it.clientContact?.machines?.contains(query, ignoreCase = true) == true)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Clienti",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(4.dp))
                val withMachines = mergedContacts.count { it.machineCount > 0 }
                Text(
                    "${mergedContacts.size} contatti \u2022 $withMachines con macchine",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Cerca contatto, macchina, seriale...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    )
                )
                Spacer(Modifier.height(8.dp))
            }

            if (filteredContacts.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.People, null,
                            Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (phoneContacts.isEmpty())
                                "Nessun contatto trovato"
                            else "Nessun risultato per \"" + query + "\"",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (phoneContacts.isEmpty()) {
                            Text(
                                "Verifica i permessi della rubrica",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            items(filteredContacts.size) { index ->
                val mc = filteredContacts[index]
                MergedContactCard(
                    mc = mc,
                    onTap = {
                        if (mc.clientContact != null) {
                            editContact = mc.clientContact
                            editPhoneContact = null
                        } else {
                            editContact = null
                            editPhoneContact = PhoneContact(mc.name, mc.phone)
                        }
                        showDialog = true
                    },
                    onDeleteMachines = {
                        mc.clientContact?.let { vm.deleteClientContact(it) }
                    }
                )
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showDialog) {
        val contactToEdit = editContact ?: editPhoneContact?.let { pc ->
            ClientContact(name = pc.name, phone = pc.phone)
        }
        if (contactToEdit != null) {
            EditClientContactDialog(
                contact = if (contactToEdit.id == 0L) null else contactToEdit,
                prefillName = contactToEdit.name,
                prefillPhone = contactToEdit.phone,
                prefillMachines = contactToEdit.machines,
                vm = vm,
                onDismiss = {
                    showDialog = false
                    editContact = null
                    editPhoneContact = null
                },
                onSave = { client ->
                    vm.upsertClientContact(client)
                    showDialog = false
                    editContact = null
                    editPhoneContact = null
                }
            )
        }
    }
}

@Composable
private fun MergedContactCard(
    mc: MergedContact,
    onTap: () -> Unit,
    onDeleteMachines: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val machines = remember(mc.clientContact?.machines) {
        val str = mc.clientContact?.machines ?: ""
        if (str.isBlank()) emptyList()
        else str.split(",").map { s ->
            val parts = s.trim().split("|")
            SerialEntry(parts.getOrElse(0) { "" }, parts.getOrElse(1) { "" })
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (machines.isNotEmpty())
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        mc.name.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (machines.isNotEmpty())
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(mc.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        mc.phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (machines.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(VNGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            machines.size.toString() + " macch.",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = VNGreen
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                }

                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp, start = 52.dp)) {
                    if (machines.isNotEmpty()) {
                        machines.forEach { m ->
                            Row(
                                modifier = Modifier.padding(bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Build, null,
                                    Modifier.size(14.dp),
                                    tint = VNGreen
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    buildString {
                                        append("SN: " + m.number)
                                        if (m.model.isNotBlank()) append(" \u2014 ${m.model}")
                                    },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    } else {
                        Text(
                            "Nessuna macchina assegnata \u2014 tocca per aggiungere",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = onTap) {
                            Icon(
                                if (machines.isEmpty()) Icons.Default.Add else Icons.Default.Edit,
                                if (machines.isEmpty()) "Aggiungi macchine" else "Modifica",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (machines.isNotEmpty()) {
                            IconButton(onClick = onDeleteMachines) {
                                Icon(
                                    Icons.Default.Delete, "Rimuovi macchine",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
