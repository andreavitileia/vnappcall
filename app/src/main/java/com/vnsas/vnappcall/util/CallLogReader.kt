package com.vnsas.vnappcall.util

import android.content.Context
import android.provider.CallLog
import com.vnsas.vnappcall.data.CallLogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CallLogReader {

    suspend fun loadRecent(context: Context, limit: Int = 100): List<CallLogEntry> =
        withContext(Dispatchers.IO) {
            val entries = mutableListOf<CallLogEntry>()
            try {
                val cursor = context.contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    arrayOf(
                        CallLog.Calls.CACHED_NAME,
                        CallLog.Calls.NUMBER,
                        CallLog.Calls.TYPE,
                        CallLog.Calls.DATE,
                        CallLog.Calls.DURATION
                    ),
                    null, null,
                    "${CallLog.Calls.DATE} DESC"
                )
                cursor?.use {
                    var count = 0
                    while (it.moveToNext() && count < limit) {
                        entries += CallLogEntry(
                            name = it.getString(0),
                            number = it.getString(1),
                            type = it.getInt(2),
                            date = it.getLong(3),
                            durationSec = it.getInt(4)
                        )
                        count++
                    }
                }
            } catch (_: Throwable) {
                // Permission not granted or other error
            }
            entries
        }

    suspend fun loadInDateRange(
        context: Context,
        fromMillis: Long,
        toMillis: Long
    ): List<CallLogEntry> = withContext(Dispatchers.IO) {
        val entries = mutableListOf<CallLogEntry>()
        try {
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(
                    CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DATE,
                    CallLog.Calls.DURATION
                ),
                "${CallLog.Calls.DATE} >= ? AND ${CallLog.Calls.DATE} <= ?",
                arrayOf(fromMillis.toString(), toMillis.toString()),
                "${CallLog.Calls.DATE} ASC"
            )
            cursor?.use {
                while (it.moveToNext()) {
                    entries += CallLogEntry(
                        name = it.getString(0),
                        number = it.getString(1),
                        type = it.getInt(2),
                        date = it.getLong(3),
                        durationSec = it.getInt(4)
                    )
                }
            }
        } catch (_: Throwable) {
            // Permission not granted or other error
        }
        entries
    }

    fun findDuration(context: Context, phone: String, timestampMs: Long): Int {
        try {
            val window = 5 * 60 * 1000L
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.DURATION),
                "${CallLog.Calls.NUMBER} LIKE ? AND ${CallLog.Calls.DATE} BETWEEN ? AND ?",
                arrayOf("%${phone.takeLast(7)}", (timestampMs - window).toString(), (timestampMs + window).toString()),
                "${CallLog.Calls.DATE} DESC"
            )
            cursor?.use {
                if (it.moveToFirst()) return it.getInt(0)
            }
        } catch (_: SecurityException) { }
        return 0
    }
}
