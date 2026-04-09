package com.vnsas.vnappcall.util

import android.util.Log
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

    private const val TAG = "PortalSync"

    suspend fun uploadReport(
        portalUrl: String,
        apiKey: String,
        dateStr: String,
        items: List<CallNote>
    ): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "uploadReport: url=$portalUrl, date=$dateStr, items=${items.size}")
        if (portalUrl.isBlank() || apiKey.isBlank()) {
            Log.e(TAG, "uploadReport: portalUrl or apiKey is blank! url='$portalUrl' key='$apiKey'")
            return@withContext false
        }
        if (!portalUrl.startsWith("http")) {
            Log.e(TAG, "uploadReport: portalUrl doesn't start with http: '$portalUrl'")
            return@withContext false
        }

        try {
            val dfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
            val rows = JSONArray()
            for (item in items) {
                // Server expects "serials" as TEXT[] (array of strings like "SN123|ModelX")
                val serialsArray = JSONArray()
                if (item.serial.isNotBlank()) {
                    item.serial.split(",").forEach { s ->
                        val trimmed = s.trim()
                        if (trimmed.isNotBlank()) {
                            serialsArray.put(trimmed)
                        }
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
                    put("serials", serialsArray)
                }
                rows.put(row)
            }

            val payload = JSONObject().apply {
                put("date", dateStr)
                put("rows", rows)
            }

            val fullUrl = "${portalUrl.trimEnd('/')}/api/reports"
            Log.d(TAG, "POST $fullUrl payload=${payload.toString().take(500)}")

            val url = URL(fullUrl)
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
            val body = try {
                if (code in 200..299) conn.inputStream.bufferedReader().readText()
                else conn.errorStream?.bufferedReader()?.readText() ?: "no body"
            } catch (_: Throwable) { "read error" }
            conn.disconnect()

            Log.d(TAG, "Response: code=$code body=$body")
            code in 200..299
        } catch (e: Throwable) {
            Log.e(TAG, "uploadReport failed", e)
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
