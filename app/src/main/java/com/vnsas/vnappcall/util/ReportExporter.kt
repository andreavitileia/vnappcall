package com.vnsas.vnappcall.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.vnsas.vnappcall.data.CallNote
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExporter {

    fun exportCsv(context: Context, items: List<CallNote>): Pair<File, Uri> {
        val dfFile = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
        val dfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
        val now = Date()
        val file = File(context.cacheDir, "report_${dfFile.format(now)}.csv")

        // Find max serial columns needed
        val maxSerials = items.maxOfOrNull { entry ->
            val s = entry.serial
            if (s.isBlank()) 0 else s.split(",").size
        } ?: 0

        file.bufferedWriter().use { w ->
            // Header
            val header = buildString {
                append("Data;Ora;Durata (mm:ss);Nome Cliente;Numero Telefono;Note;Da Fatturare;Risolto")
                for (i in 1..maxSerials) {
                    append(";Seriale $i;Modello Macchina $i")
                }
            }
            w.write(header)
            w.newLine()

            // Rows
            for (item in items) {
                val date = Date(item.timestamp)
                val durMin = item.durationSec / 60
                val durSec = item.durationSec % 60
                val duration = "%02d:%02d".format(durMin, durSec)
                val billable = if (item.billable) "SI" else "NO"
                val resolved = if (item.resolved) "SI" else "NO"

                val row = buildString {
                    append("${dfDate.format(date)};${dfTime.format(date)};$duration")
                    append(";${item.contactName};${item.phone}")
                    append(";${item.note.replace(";", ",")};$billable;$resolved")

                    val serials = if (item.serial.isBlank()) emptyList()
                    else item.serial.split(",").map { it.trim() }

                    for (i in 0 until maxSerials) {
                        if (i < serials.size) {
                            val parts = serials[i].split("|")
                            append(";${parts.getOrElse(0) { "" }};${parts.getOrElse(1) { "" }}")
                        } else {
                            append(";;")
                        }
                    }
                }
                w.write(row)
                w.newLine()
            }
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return file to uri
    }
}
