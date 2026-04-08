package com.vnsas.vnappcall.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vnsas.vnappcall.MainViewModel
import com.vnsas.vnappcall.data.CallLogEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsTab(vm: MainViewModel) {
    val callLog by vm.callLog.collectAsState()
    val contacts by vm.contacts.collectAsState()
    var query by remember { mutableStateOf("") }

    val filteredLog = remember(callLog, query) {
        if (query.isBlank()) callLog
        else callLog.filter {
            (it.name ?: "").contains(query, ignoreCase = true) ||
            (it.number ?: "").contains(query)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Contatti & Registro") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Cerca contatto o numero...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (filteredLog.isEmpty()) {
                Text(
                    "Nessun risultato",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(top = 32.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }

            LazyColumn {
                items(filteredLog) { entry ->
                    CallLogItem(entry)
                    Divider()
                }
            }
        }
    }
}

@Composable
private fun CallLogItem(entry: CallLogEntry) {
    val df = remember { SimpleDateFormat("dd/MM HH:mm", Locale.ITALIAN) }
    val durMin = entry.durationSec / 60
    val durSec = entry.durationSec % 60

    ListItem(
        headlineContent = {
            Text(entry.name ?: entry.number ?: "Sconosciuto")
        },
        supportingContent = {
            Text(
                "${entry.number ?: "—"}  •  ${entry.readableType}  •  %02d:%02d".format(durMin, durSec)
            )
        },
        trailingContent = {
            Text(
                df.format(Date(entry.date)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}
