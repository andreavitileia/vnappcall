package com.vnsas.vnappcall.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vnsas.vnappcall.MainViewModel
import com.vnsas.vnappcall.data.ClientContact

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientContactPickerDialog(
    vm: MainViewModel,
    onDismiss: () -> Unit,
    onSelect: (ClientContact) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clientContacts by vm.clientContacts.collectAsState()
    var query by remember { mutableStateOf("") }

    val filtered = remember(clientContacts, query) {
        if (query.isBlank()) clientContacts
        else clientContacts.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.phone.contains(query) ||
            it.machines.contains(query, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                "Seleziona cliente",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Cerca cliente...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (filtered.isEmpty()) {
                Text(
                    if (clientContacts.isEmpty()) "Nessun cliente registrato.\nVai al tab Clienti per aggiungerne."
                    else "Nessun risultato",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp, bottom = 24.dp)
                )
            }

            LazyColumn {
                items(filtered, key = { it.id }) { contact ->
                    val machineCount = if (contact.machines.isBlank()) 0
                        else contact.machines.split(",").size

                    ListItem(
                        headlineContent = {
                            Text(contact.name, fontWeight = FontWeight.SemiBold)
                        },
                        supportingContent = {
                            Column {
                                if (contact.phone.isNotBlank()) {
                                    Text(contact.phone)
                                }
                                if (machineCount > 0) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Build,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 4.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            "$machineCount macchine",
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier.clickable { onSelect(contact) }
                    )
                    Divider()
                }
            }
        }
    }
}
