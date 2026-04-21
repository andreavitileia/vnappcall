package com.vnsas.vnappcall.ui

import android.content.Intent
import android.provider.CallLog
import android.provider.ContactsContract
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
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vnsas.vnappcall.MainViewModel
import com.vnsas.vnappcall.data.CallLogEntry
import com.vnsas.vnappcall.data.CallNote
import com.vnsas.vnappcall.ui.theme.VNGreen
import com.vnsas.vnappcall.ui.theme.VNOrange
import com.vnsas.vnappcall.ui.theme.VNRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsTab(vm: MainViewModel) {
    val callLog by vm.callLog.collectAsState()
    val allNotes by vm.allNotes.collectAsState()
    val syncStatus by vm.syncStatus.collectAsState()
    var filter by remember { mutableIntStateOf(0) }
    var showDialog by remember { mutableStateOf(false) }
    var editNote by remember { mutableStateOf<CallNote?>(null) }
    var prefilledEntry by remember { mutableStateOf<CallLogEntry?>(null) }

    LaunchedEffect(Unit) {
        vm.refreshCallLog()
    }

    val filtered = remember(callLog, filter) {
        when (filter) {
            1 -> callLog.filter { it.type == CallLog.Calls.INCOMING_TYPE }
            2 -> callLog.filter { it.type == CallLog.Calls.OUTGOING_TYPE }
            3 -> callLog.filter { it.type == CallLog.Calls.MISSED_TYPE }
            else -> callLog
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Chiamate",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    IconButton(onClick = { vm.refreshCallLog() }) {
                        Icon(
                            Icons.Default.Refresh, "Aggiorna",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "${'$'}{callLog.size} chiamate recenti",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                syncStatus?.let { status ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        status,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (status.startsWith("Errore")) VNRed else VNGreen
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryCard(
                        "Entranti",
                        callLog.count { it.type == CallLog.Calls.INCOMING_TYPE }.toString(),
                        VNGreen, Modifier.weight(1f)
                    )
                    SummaryCard(
                        "Uscenti",
                        callLog.count { it.type == CallLog.Calls.OUTGOING_TYPE }.toString(),
                        MaterialTheme.colorScheme.primary, Modifier.weight(1f)
                    )
                    SummaryCard(
                        "Perse",
                        callLog.count { it.type == CallLog.Calls.MISSED_TYPE }.toString(),
                        VNRed, Modifier.weight(1f)
                    )
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Tutte", "Entranti", "Uscenti", "Perse").forEachIndexed { idx, label ->
                        FilterChip(
                            selected = filter == idx,
                            onClick = { filter = idx },
                            label = { Text(label, style = MaterialTheme.typography.labelLarge) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            if (filtered.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Phone, null,
                            Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Nessuna chiamata trovata",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Le chiamate appariranno automaticamente",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            items(filtered) { entry ->
                val existingNote = allNotes.find { n ->
                    n.phone.takeLast(7) == (entry.number ?: "").takeLast(7) &&
                    kotlin.math.abs(n.timestamp - entry.date) < 5 * 60 * 1000L
                }
                val isUnknown = entry.name == null || entry.name.isBlank()
                CallLogCard(
                    entry = entry,
                    note = existingNote,
                    isUnknown = isUnknown,
                    onAnnotate = {
                        prefilledEntry = entry
                        editNote = existingNote
                        showDialog = true
                    },
                    onDeleteNote = { existingNote?.let { vm.deleteNote(it) } }
                )
            }
            item { Spacer(Modifier.height(80.dp)) }
        }

        FloatingActionButton(
            onClick = { prefilledEntry = null; editNote = null; showDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape
        ) { Icon(Icons.Default.Add, "Nuova nota") }
    }

    if (showDialog) {
        val prefilled = prefilledEntry
        val initialNote = editNote ?: if (prefilled != null) {
            CallNote(
                contactName = prefilled.name ?: "",
                phone = prefilled.number ?: "",
                timestamp = prefilled.date,
                durationSec = prefilled.durationSec
            )
        } else null

        EditCallDialog(
            note = initialNote,
            vm = vm,
            onDismiss = { showDialog = false; prefilledEntry = null },
            onSave = { vm.upsertNote(it); showDialog = false; prefilledEntry = null }
        )
    }
}

@Composable
private fun SummaryCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = color)
            Spacer(Modifier.height(2.dp))
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CallLogCard(
    entry: CallLogEntry,
    note: CallNote?,
    isUnknown: Boolean,
    onAnnotate: () -> Unit,
    onDeleteNote: () -> Unit
) {
    val df = remember { SimpleDateFormat("dd/MM HH:mm", Locale.ITALIAN) }
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val (icon, iconColor) = when (entry.type) {
        CallLog.Calls.INCOMING_TYPE -> Icons.Default.CallReceived to VNGreen
        CallLog.Calls.OUTGOING_TYPE -> Icons.Default.CallMade to MaterialTheme.colorScheme.primary
        CallLog.Calls.MISSED_TYPE -> Icons.Default.CallMissed to VNRed
        else -> Icons.Default.Phone to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (note != null)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, Modifier.size(20.dp), tint = iconColor)
                }
                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        entry.name ?: entry.number ?: "Sconosciuto",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            entry.readableType,
                            style = MaterialTheme.typography.bodySmall,
                            color = iconColor
                        )
                        Text(
                            "  \u2022  " + df.format(Date(entry.date)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (entry.durationSec > 0) {
                            Text(
                                "  \u2022  %d:%02d".format(entry.durationSec / 60, entry.durationSec % 60),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (note != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (note.billable) {
                            Text(
                                "\u20AC",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(VNOrange)
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                        if (note.resolved) {
                            Text(
                                "\u2713",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(VNGreen)
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                        if (note.serial.isNotBlank()) {
                            Text(
                                "\uD83D\uDD27",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp, start = 52.dp)) {
                    if (entry.number != null && entry.name != null) {
                        Text(
                            entry.number,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isUnknown && entry.number != null) {
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
                                    type = ContactsContract.RawContacts.CONTENT_TYPE
                                    putExtra(ContactsContract.Intents.Insert.PHONE, entry.number)
                                }
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PersonAdd, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Aggiungi a rubrica")
                        }
                    }

                    if (note != null) {
                        Spacer(Modifier.height(6.dp))
                        if (note.note.isNotBlank()) {
                            Text(
                                "Nota: ${'$'}{note.note}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        if (note.serial.isNotBlank()) {
                            val machinesList = note.serial.split(",").map { s ->
                                val parts = s.trim().split("|")
                                val sn = parts.getOrElse(0) { "" }
                                val model = parts.getOrElse(1) { "" }
                                if (model.isNotBlank()) "${'$'}model (SN: ${'$'}sn)" else "SN: ${'$'}sn"
                            }
                            Column(modifier = Modifier.padding(bottom = 4.dp)) {
                                Text(
                                    "Macchine:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = VNGreen,
                                    fontWeight = FontWeight.Bold
                                )
                                machinesList.forEach { machine ->
                                    Text(
                                        "  \u2022 ${'$'}machine",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (note.billable) Text(
                                "\u20AC Da fatturare",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(VNOrange)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                            if (note.resolved) Text(
                                "\u2713 Risolto",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(VNGreen)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = onAnnotate) {
                            Icon(
                                Icons.Default.Edit,
                                if (note == null) "Annota" else "Modifica",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (note != null) {
                            IconButton(onClick = onDeleteNote) {
                                Icon(
                                    Icons.Default.Delete, "Elimina nota",
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
