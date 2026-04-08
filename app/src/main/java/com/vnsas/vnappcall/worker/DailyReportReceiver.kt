package com.vnsas.vnappcall.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.vnsas.vnappcall.data.AppDatabase
import com.vnsas.vnappcall.data.loadMailSettings
import com.vnsas.vnappcall.util.MailSender
import com.vnsas.vnappcall.util.PortalSync
import com.vnsas.vnappcall.util.ReportExporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class DailyReportReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "DailyReport"
        const val ACTION = "com.vnsas.vnappcall.ACTION_DAILY_REPORT"
        private const val REQUEST_CODE = 1001

        fun scheduleDailyExact(context: Context, hour: Int, minute: Int) {
            val now = Calendar.getInstance()
            val runAt = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (!runAt.after(now)) runAt.add(Calendar.DAY_OF_YEAR, 1)

            val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, DailyReportReceiver::class.java).apply { action = ACTION }
            val pi = PendingIntent.getBroadcast(
                context, REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmMgr.cancel(pi)

            if (Build.VERSION.SDK_INT >= 31) {
                val am = context.getSystemService(AlarmManager::class.java)
                if (am != null && am.canScheduleExactAlarms()) {
                    alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, runAt.timeInMillis, pi)
                    Log.d(TAG, "setExactAndAllowWhileIdle for ${runAt.time}")
                } else {
                    val showPi = PendingIntent.getActivity(
                        context, REQUEST_CODE + 1,
                        Intent("android.intent.action.SHOW_ALARMS"),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    val info = AlarmManager.AlarmClockInfo(runAt.timeInMillis, showPi)
                    alarmMgr.setAlarmClock(info, pi)
                    Log.w(TAG, "setAlarmClock for ${runAt.time}")
                }
            } else {
                alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, runAt.timeInMillis, pi)
                Log.d(TAG, "setExactAndAllowWhileIdle for ${runAt.time}")
            }
        }

        fun cancel(context: Context) {
            val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, DailyReportReceiver::class.java).apply { action = ACTION }
            val pi = PendingIntent.getBroadcast(
                context, REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmMgr.cancel(pi)
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION) return
        Log.d(TAG, "Daily report triggered")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = context.loadMailSettings()
                val dao = AppDatabase.getInstance(context).callNoteDao()

                // Get today's notes
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, 1)
                val end = cal.timeInMillis
                val notes = dao.getBetween(start, end)

                if (notes.isNotEmpty()) {
                    // Send email
                    if (settings.host.isNotBlank() && settings.to.isNotBlank()) {
                        val (file, _) = ReportExporter.exportCsv(context, notes)
                        MailSender.sendReportEmail(
                            settings,
                            "Report giornaliero VNAppCall",
                            "Report automatico di oggi in allegato.",
                            file
                        )
                        Log.d(TAG, "Email sent")
                    }

                    // Upload to portal
                    if (settings.portalUrl.isNotBlank()) {
                        val dateStr = PortalSync.formatDate(notes.first().timestamp)
                        PortalSync.uploadReport(settings.portalUrl, settings.apiKey, dateStr, notes)
                        Log.d(TAG, "Portal sync done")
                    }
                }

                // Reschedule for tomorrow
                scheduleDailyExact(context, settings.autoHour, settings.autoMinute)
            } catch (e: Exception) {
                Log.e(TAG, "Error in daily report", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
