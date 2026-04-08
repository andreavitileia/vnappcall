package com.vnsas.vnappcall.util

import com.vnsas.vnappcall.data.CallNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PortalSync {

    suspend fun uploadReport(
        portalUrl: String,
        apiKey: String,
        dateStr: String,
        items: List<CallNote>
    ): Boolean = withContext(Dispatchers.IO) {
        if (portalUrl.isBlank() || apiKey.isBlank()) return@withContext false
        if (!portalUrl.startsWith("http")) return@withContext false

        try {
            val dfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
            val rows = JSONArray()
            for (item in items) {
                val machines = JSONArray()
                if (item.serial.isNotBlank()) {
                    item.serial.split(",").forEach { s ->
                        val parts = s.trim().split("|")
                        val machine = JSONObject().apply {
                            put("serial", parts.getOrElse(0) { "" })
                            put("model", parts.getOrElse(1) { "" })
                        }
                        machines.put(machine)
                    }
                }

                val row = JSONObject().apply {
                    put("timestamp", item.timestamp)
                    put("time", dfTime.format(Date(item.timestamp)))
                    put("contactName", item.contactName)
                    put("phone", item.phone)
                    put("durationSec", item.durationSec)
                    put("durationFormatted", "%d:%02d".format(
                        item.durationSec / 60, item.durationSec % 60
                    ))
                    put("note", item.note)
                    put("billable", item.billable)
                    put("resolved", item.resolved)
                    put("machines", machines)
                }
                rows.put(row)
            }

            val payload = JSONObject().apply {
                put("date", dateStr)
                put("totalCalls", items.size)
                put("billableCount", items.count { it.billable })
                put("resolvedCount", items.count { it.resolved })
                put("rows", rows)
            }

            val url = URL("${portalUrl.trimEnd('/')}/api/reports")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("X-Reports-Api-Key", apiKey)
                connectTimeout = 15_000
                readTimeout = 15_000
                doOutput = true
            }

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use {
                it.write(payload.toString())
            }

            val code = conn.responseCode
            conn.disconnect()
            code in 200..299
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun bulkSync(
        portalUrl: String,
        apiKey: String,
        notesByDate: Map<String, List<CallNote>>,
        onProgress: (current: Int, total: Int) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        var success = 0
        val dates = notesByDate.keys.sorted()
        for ((i, date) in dates.withIndex()) {
            val items = notesByDate[date] ?: continue
            try {
                if (uploadReport(portalUrl, apiKey, date, items)) success++
            } catch (_: Throwable) {
                // Skip failed dates, continue with rest
            }
            onProgress(i + 1, dates.size)
        }
        success
    }

    fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
    }
}
