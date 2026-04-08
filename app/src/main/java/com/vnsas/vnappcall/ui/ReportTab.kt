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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vnsas.vnappcall.MainViewModel
import com.vnsas.vnappcall.ui.theme.VNGreen
import com.vnsas.vnappcall.ui.theme.VNOrange
import com.vnsas.vnappcall.util.ReportExporter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ReportTab(vm: MainViewModel) {
    val todayNotes by vm.todayNotes.collectAsState()
    val allNotes by vm.allNotes.collectAsState()
    val context = LocalContext.current
    val today = SimpleDateFormat("dd MMMM yyyy", Locale.ITALIAN).format(Date())
    val billableToday = todayNotes.count { it.billable }
    val resolvedToday = todayNotes.count { it.resolved }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            "Report",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Today summary card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    today,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "${todayNotes.size} note registrate",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "$billableToday da fatturare  \u2022  $resolvedToday risolte",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }

        // Statistics card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Statistiche",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem("Totale", "${allNotes.size}", MaterialTheme.colorScheme.primary)
                    StatItem("Oggi", "${todayNotes.size}", MaterialTheme.colorScheme.primary)
                    StatItem("Da fatt.", "${allNotes.count { it.billable }}", VNOrange)
                    StatItem("Risolte", "${allNotes.count { it.resolved }}", VNGreen)
                }
            }
        }

        // Actions section
        Text(
            "Azioni",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 4.dp)
        )

        // Export CSV
        Button(
            onClick = {
                try {
                    if (todayNotes.isNotEmpty()) {
                        val (_, uri) = ReportExporter.exportCsv(context, todayNotes)
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Condividi report"))
                    }
                } catch (_: Exception) { }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = todayNotes.isNotEmpty(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.Share, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Esporta CSV di oggi")
        }

        // Send email
        OutlinedButton(
            onClick = { vm.sendReport(todayNotes) },
            modifier = Modifier.fillMaxWidth(),
            enabled = todayNotes.isNotEmpty(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Email, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Invia report via email")
        }

        // Sync to portal
        OutlinedButton(
            onClick = { vm.syncToPortal(todayNotes) },
            modifier = Modifier.fillMaxWidth(),
            enabled = todayNotes.isNotEmpty(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.CloudUpload, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Sincronizza con portale")
        }

        // Bulk sync
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Sincronizzazione massiva",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Invia tutte le note degli ultimi 30 giorni al portale",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        try {
                            val cal = Calendar.getInstance()
                            val end = cal.timeInMillis
                            cal.add(Calendar.DAY_OF_YEAR, -30)
                            val start = cal.timeInMillis
                            vm.bulkSyncToPortal(start, end)
                        } catch (_: Exception) { }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Sync, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Avvia bulk sync (30 giorni)")
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StatItem(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
