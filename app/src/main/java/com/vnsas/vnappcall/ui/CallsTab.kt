package com.vnsas.vnappcall.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vnsas.vnappcall.MainViewModel
import com.vnsas.vnappcall.data.CallNote
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsTab(vm: MainViewModel) {
    val todayNotes by vm.todayNotes.collectAsState()
    val allNotes by vm.allNotes.collectAsState()
    var filter by remember { mutableIntStateOf(0) }
    var showDialog by remember { mutableStateOf(false) }
    var editNote by remember { mutableStateOf<CallNote?>(null) }

    val notes = if (filter == 0) todayNotes else allNotes

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chiamate") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editNote = null
                showDialog = true
            }) {
                Icon(Icons.Default.Add, "Nuova nota")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Oggi", "Tutte").forEachIndexed { idx, label ->
                    FilterChip(
                        selected = filter == idx,
                        onClick = { filter = idx },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (notes.isEmpty()) {
                Text(
                    text = if (filter == 0) "Nessuna chiamata oggi" else "Nessuna chiamata registrata",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 32.dp).align(Alignment.CenterHorizontally)
                )
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    CallNoteCard(
                        note = note,
                        onEdit = {
                            editNote = note
                            showDialog = true
                        },
                        onDelete = { vm.deleteNote(note) }
                    )
                }
            }
        }
    }

    if (showDialog) {
        EditCallDialog(
            note = editNote,
            vm = vm,
            onDismiss = { showDialog = false },
            onSave = { saved ->
                vm.upsertNote(saved)
                showDialog = false
            }
        )
    }
}

@Composable
private fun CallNoteCard(
    note: CallNote,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val df = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALIAN) }
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = note.contactName.ifBlank { "Sconosciuto" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${note.phone}  •  ${df.format(Date(note.timestamp))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    if (note.billable) {
                        Text(
                            "€",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                    if (note.resolved) {
                        Text(
                            "OK",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    if (note.note.isNotBlank()) {
                        Text(
                            text = note.note,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    if (note.serial.isNotBlank()) {
                        Text(
                            text = "Seriali: ${note.serial}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (note.durationSec > 0) {
                        val min = note.durationSec / 60
                        val sec = note.durationSec % 60
                        Text(
                            text = "Durata: %02d:%02d".format(min, sec),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, "Modifica", tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.width(4.dp))
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, "Elimina", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}
