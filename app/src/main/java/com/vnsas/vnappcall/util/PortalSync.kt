package com.vnsas.vnappcall.util

import android.util.Log
import com.vnsas.vnappcall.data.CallLogEntry
import com.vnsas.vnappcall.data.CallNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object PortalSync {

    private const val TAG = "PortalSync"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    suspend fun uploadReport(
        portalUrl: String,
        apiKey: String,
        dateStr: String,
        items: List<CallNote>
    ): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "uploadReport called: url='$portalUrl', apiKey='${apiKey.take(5)}...', date=$dateStr, items=${items.size}")

        if (portalUrl.isBlank() || apiKey.isBlank()) {
            Log.e(TAG, "ABORT: portalUrl or apiKey is blank! url='$portalUrl' key='$apiKey'")
            return@withContext false
        }
        if (!portalUrl.startsWith("http")) {
            Log.e(TAG, "ABORT: portalUrl doesn't start with http: '$portalUrl'")
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
            val payloadStr = payload.toString()
            Log.d(TAG, "POST $fullUrl")
            Log.d(TAG, "Payload (first 500): ${payloadStr.take(500)}")

            val request = Request.Builder()
                .url(fullUrl)
                .addHeader("Content-Type", "application/json; charset=UTF-8")
                .addHeader("X-Reports-Api-Key", apiKey)
                .post(payloadStr.toRequestBody(JSON_MEDIA))
                .build()

            val response = httpClient.newCall(request).execute()
            val code = response.code
            val body = response.body?.string() ?: "empty body"
            response.close()

            Log.d(TAG, "Response: code=$code body=$body")

            if (code in 200..299) {
                Log.d(TAG, "SUCCESS: Upload completed")
                true
            } else {
                Log.e(TAG, "FAILED: HTTP $code - $body")
                false
            }
        } catch (e: Throwable) {
            Log.e(TAG, "EXCEPTION in uploadReport: ${e.javaClass.simpleName}: ${e.message}", e)
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
        Log.d(TAG, "bulkSync: ${dates.size} dates to sync")
        for ((i, date) in dates.withIndex()) {
            val items = notesByDate[date] ?: continue
            try {
                Log.d(TAG, "bulkSync: syncing date $date with ${items.size} items")
                if (uploadReport(portalUrl, apiKey, date, items)) {
                    success++
                    Log.d(TAG, "bulkSync: date $date OK")
                } else {
                    Log.e(TAG, "bulkSync: date $date FAILED")
                }
            } catch (e: Throwable) {
                Log.e(TAG, "bulkSync: date $date EXCEPTION: ${e.message}", e)
            }
            onProgress(i + 1, dates.size)
        }
        Log.d(TAG, "bulkSync complete: $success/${dates.size} succeeded")
        success
    }

    /**
     * Upload all calls (from phone log, merged with any annotations) for a date.
     * This sends ALL calls to the portal, not just annotated ones.
     */
    suspend fun uploadAllCalls(
        portalUrl: String,
        apiKey: String,
        dateStr: String,
        callLogEntries: List<CallLogEntry>,
        annotations: List<CallNote>
    ): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "uploadAllCalls: url='$portalUrl', date=$dateStr, calls=${callLogEntries.size}, annotations=${annotations.size}")

        if (portalUrl.isBlank() || apiKey.isBlank()) {
            Log.e(TAG, "ABORT: portalUrl or apiKey is blank!")
            return@withContext false
        }
        if (!portalUrl.startsWith("http")) {
            Log.e(TAG, "ABORT: portalUrl doesn't start with http: '$portalUrl'")
            return@withContext false
        }

        try {
            val dfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
            val rows = JSONArray()

            // Build a lookup of annotations by phone suffix + timestamp proximity
            val annotationMap = mutableMapOf<String, CallNote>()
            for (note in annotations) {
                val key = note.phone.replace(Regex("[^0-9+]"), "").takeLast(7)
                if (key.length >= 4) annotationMap[key + "_" + (note.timestamp / 300000)] = note
            }

            for (entry in callLogEntries) {
                val phoneDigits = (entry.number ?: "").replace(Regex("[^0-9+]"), "")
                val phoneSuffix = phoneDigits.takeLast(7)
                val timeKey = phoneSuffix + "_" + (entry.date / 300000)
                val note = annotationMap[timeKey]

                val serialsArray = JSONArray()
                if (note != null && note.serial.isNotBlank()) {
                    note.serial.split(",").forEach { s ->
                        val trimmed = s.trim()
                        if (trimmed.isNotBlank()) serialsArray.put(trimmed)
                    }
                }

                val contactName = entry.name ?: note?.contactName ?: (entry.number ?: "Sconosciuto")
                val phone = entry.number ?: note?.phone ?: ""

                val row = JSONObject().apply {
                    put("timestamp", entry.date)
                    put("time", dfTime.format(Date(entry.date)))
                    put("contactName", contactName)
                    put("phone", phone)
                    put("durationSec", entry.durationSec)
                    put("durationFormatted", "%d:%02d".format(
                        entry.durationSec / 60, entry.durationSec % 60
                    ))
                    put("note", note?.note ?: "")
                    put("billable", note?.billable ?: false)
                    put("resolved", note?.resolved ?: false)
                    put("serials", serialsArray)
                    put("callType", entry.readableType)
                }
                rows.put(row)
            }

            val payload = JSONObject().apply {
                put("date", dateStr)
                put("rows", rows)
            }

            val fullUrl = "${portalUrl.trimEnd('/')}/api/reports"
            val payloadStr = payload.toString()
            Log.d(TAG, "POST $fullUrl (${callLogEntries.size} calls)")
            Log.d(TAG, "Payload (first 500): ${payloadStr.take(500)}")

            val request = Request.Builder()
                .url(fullUrl)
                .addHeader("Content-Type", "application/json; charset=UTF-8")
                .addHeader("X-Reports-Api-Key", apiKey)
                .post(payloadStr.toRequestBody(JSON_MEDIA))
                .build()

            val response = httpClient.newCall(request).execute()
            val code = response.code
            val body = response.body?.string() ?: "empty body"
            response.close()

            Log.d(TAG, "uploadAllCalls Response: code=$code body=$body")

            if (code in 200..299) {
                Log.d(TAG, "uploadAllCalls SUCCESS: ${callLogEntries.size} calls uploaded")
                true
            } else {
                Log.e(TAG, "uploadAllCalls FAILED: HTTP $code - $body")
                false
            }
        } catch (e: Throwable) {
            Log.e(TAG, "uploadAllCalls EXCEPTION: ${e.javaClass.simpleName}: ${e.message}", e)
            false
        }
    }

    fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
    }
}
