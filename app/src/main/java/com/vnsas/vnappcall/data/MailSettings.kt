package com.vnsas.vnappcall.data

import android.content.Context

data class MailSettings(
    val host: String = "",
    val port: String = "587",
    val ssl: Boolean = true,
    val user: String = "",
    val pass: String = "",
    val from: String = "",
    val to: String = "",
    val autoEnabled: Boolean = false,
    val autoHour: Int = 8,
    val autoMinute: Int = 0,
    val portalUrl: String = "https://king-prawn-app-ugucb.ondigitalocean.app",
    val apiKey: String = "vnappcall-reports-key-2025"
)

private const val PREFS = "mail_prefs"

fun Context.loadMailSettings(): MailSettings {
    val p = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    return MailSettings(
        host = p.getString("host", "") ?: "",
        port = p.getString("port", "587") ?: "587",
        ssl = p.getBoolean("ssl", true),
        user = p.getString("user", "") ?: "",
        pass = p.getString("pass", "") ?: "",
        from = p.getString("from", "") ?: "",
        to = p.getString("to", "") ?: "",
        autoEnabled = p.getBoolean("autoEnabled", false),
        autoHour = p.getInt("autoHour", 8),
        autoMinute = p.getInt("autoMinute", 0),
        portalUrl = p.getString("portalUrl", "https://king-prawn-app-ugucb.ondigitalocean.app") ?: "https://king-prawn-app-ugucb.ondigitalocean.app",
        apiKey = p.getString("apiKey", "vnappcall-reports-key-2025") ?: "vnappcall-reports-key-2025"
    )
}

fun Context.saveMailSettings(s: MailSettings) {
    getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply {
        putString("host", s.host)
        putString("port", s.port)
        putBoolean("ssl", s.ssl)
        putString("user", s.user)
        putString("pass", s.pass)
        putString("from", s.from)
        putString("to", s.to)
        putBoolean("autoEnabled", s.autoEnabled)
        putInt("autoHour", s.autoHour)
        putInt("autoMinute", s.autoMinute)
        putString("portalUrl", s.portalUrl)
        putString("apiKey", s.apiKey)
        apply()
    }
}
