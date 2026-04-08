package com.vnsas.vnappcall.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.vnsas.vnappcall.data.loadMailSettings

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        Log.d("BootReceiver", "Boot completed, rescheduling daily report")
        val settings = context.loadMailSettings()
        if (settings.autoEnabled) {
            DailyReportReceiver.scheduleDailyExact(context, settings.autoHour, settings.autoMinute)
        }
    }
}
