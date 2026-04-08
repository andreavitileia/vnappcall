package com.vnsas.vnappcall.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vnsas.vnappcall.MainViewModel
import com.vnsas.vnappcall.util.ReportExporter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportTab(vm: MainViewModel) {
    val todayNotes by vm.todayNotes.collectAsState()
    val allNotes by vm.allNotes.collectAsState()
    val context = LocalContext.current
    val today = SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN).format(Date())

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Report") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Today summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Oggi — $today",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${todayNotes.size} chiamate registrate",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    val billableCount = todayNotes.count { it.billable }
                    val resolvedCount = todayNotes.count { it.resolved }
                    Text(
                        "$billableCount da fatturare  •  $resolvedCount risolte",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            // Export CSV
            Button(
                onClick = {
                    if (todayNotes.isNotEmpty()) {
                        val (_, uri) = ReportExporter.exportCsv(context, todayNotes)
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Condividi report"))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = todayNotes.isNotEmpty()
            ) {
                Icon(Icons.Default.Share, null)
                Spacer(Modifier.padding(start = 8.dp))
                Text("Esporta CSV di oggi")
            }

            // Send email
            OutlinedButton(
                onClick = { vm.sendReport(todayNotes) },
                modifier = Modifier.fillMaxWidth(),
                enabled = todayNotes.isNotEmpty()
            ) {
                Icon(Icons.Default.Email, null)
                Spacer(Modifier.padding(start = 8.dp))
                Text("Invia report via email")
            }

            // Sync to portal
            OutlinedButton(
                onClick = { vm.syncToPortal(todayNotes) },
                modifier = Modifier.fillMaxWidth(),
                enabled = todayNotes.isNotEmpty()
            ) {
                Icon(Icons.Default.CloudUpload, null)
                Spacer(Modifier.padding(start = 8.dp))
                Text("Sincronizza con portale")
            }

            // Bulk sync section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Sincronizzazione massiva",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Invia tutti i dati degli ultimi 30 giorni al portale",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val cal = Calendar.getInstance()
                            val end = cal.timeInMillis
                            cal.add(Calendar.DAY_OF_YEAR, -30)
                            val start = cal.timeInMillis
                            vm.bulkSyncToPortal(start, end)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Sync, null)
                        Spacer(Modifier.padding(start = 8.dp))
                        Text("Avvia bulk sync (30 giorni)")
                    }
                }
            }

            // Stats
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Statistiche",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItem("Totale", "${allNotes.size}")
                        StatItem("Oggi", "${todayNotes.size}")
                        StatItem("Da fatturare", "${allNotes.count { it.billable }}")
                        StatItem("Risolte", "${allNotes.count { it.resolved }}")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
