package com.vnsas.vnappcall

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vnsas.vnappcall.data.CallNote
import com.vnsas.vnappcall.data.CallNoteDao
import com.vnsas.vnappcall.data.ClientContact
import com.vnsas.vnappcall.data.ClientContactDao
import com.vnsas.vnappcall.data.MailSettings
import com.vnsas.vnappcall.data.loadMailSettings
import com.vnsas.vnappcall.data.saveMailSettings
import com.vnsas.vnappcall.util.CallLogReader
import com.vnsas.vnappcall.data.CallLogEntry
import com.vnsas.vnappcall.util.ContactsReader
import com.vnsas.vnappcall.util.MailSender
import com.vnsas.vnappcall.util.PhoneContact
import com.vnsas.vnappcall.util.PortalSync
import com.vnsas.vnappcall.util.ReportExporter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val dao: CallNoteDao = (application as VNApp).database.callNoteDao()
    private val contactDao: ClientContactDao = (application as VNApp).database.clientContactDao()
    private val ctx get() = getApplication<VNApp>()

    // --- Call notes ---
    val allNotes: StateFlow<List<CallNote>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val todayNotes: StateFlow<List<CallNote>> = run {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val end = cal.timeInMillis
        dao.observeDay(start, end)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    }

    fun upsertNote(note: CallNote) {
        viewModelScope.launch {
            dao.upsert(note)
            // Auto-sync to portal in real-time
            autoSyncDate(note.timestamp)
        }
    }

    fun deleteNote(note: CallNote) {
        viewModelScope.launch {
            dao.delete(note)
            // Auto-sync to portal after deletion
            autoSyncDate(note.timestamp)
        }
    }

    /**
     * Automatically sync all calls for a given date to the portal.
     * Called after every save/update/delete for real-time sync.
     * Sends ALL calls from phone log merged with annotations.
     */
    private suspend fun autoSyncDate(timestamp: Long) {
        try {
            val settings = ctx.loadMailSettings()
            if (settings.portalUrl.isBlank() || settings.apiKey.isBlank()) return

            val dateStr = PortalSync.formatDate(timestamp)
            val cal = Calendar.getInstance().apply {
                timeInMillis = timestamp
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dayStart = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val dayEnd = cal.timeInMillis

            // Get ALL calls from phone log for this date
            val dayCalls = CallLogReader.loadInDateRange(ctx, dayStart, dayEnd)
            // Get annotations from DB
            val dayNotes = dao.getBetween(dayStart, dayEnd)
            Log.d("MainViewModel", "autoSync: date=$dateStr, calls=${dayCalls.size}, notes=${dayNotes.size}")

            if (dayCalls.isEmpty() && dayNotes.isEmpty()) {
                // Send empty report to clear the date on the portal
                PortalSync.uploadReport(settings.portalUrl, settings.apiKey, dateStr, emptyList())
            } else if (dayCalls.isNotEmpty()) {
                // Upload all calls merged with annotations
                val ok = PortalSync.uploadAllCalls(
                    settings.portalUrl, settings.apiKey, dateStr,
                    dayCalls, dayNotes
                )
                if (ok) {
                    Log.d("MainViewModel", "autoSync OK: $dateStr (${dayCalls.size} calls, ${dayNotes.size} annotations)")
                } else {
                    Log.e("MainViewModel", "autoSync FAILED: $dateStr")
                }
            } else {
                // Only annotations, no call log entries (edge case)
                val ok = PortalSync.uploadReport(settings.portalUrl, settings.apiKey, dateStr, dayNotes)
                if (ok) {
                    Log.d("MainViewModel", "autoSync OK: $dateStr (${dayNotes.size} annotations only)")
                } else {
                    Log.e("MainViewModel", "autoSync FAILED: $dateStr")
                }
            }
        } catch (e: Throwable) {
            Log.e("MainViewModel", "autoSync error", e)
        }
    }

    // --- Call log ---
    private val _callLog = MutableStateFlow<List<CallLogEntry>>(emptyList())
    val callLog: StateFlow<List<CallLogEntry>> = _callLog.asStateFlow()

    // Track if initial auto-sync has been done
    private var initialSyncDone = false

    // Sync status for UI feedback
    private val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus: StateFlow<String?> = _syncStatus.asStateFlow()
    fun clearSyncStatus() { _syncStatus.value = null }

    fun refreshCallLog() {
        viewModelScope.launch {
            _callLog.value = CallLogReader.loadRecent(ctx, 100)
            // Auto-sync all calls to portal whenever call log is refreshed
            if (!initialSyncDone) {
                initialSyncDone = true
                syncAllCallsToPortal()
                // Start periodic sync every 5 minutes
                startPeriodicSync()
            }
        }
    }

    /**
     * Called from Application.onCreate() to ensure sync starts immediately,
     * even before any UI tab is shown.
     */
    fun initAutoSync() {
        if (!initialSyncDone) {
            initialSyncDone = true
            viewModelScope.launch {
                _callLog.value = CallLogReader.loadRecent(ctx, 100)
                syncAllCallsToPortal()
                startPeriodicSync()
            }
        }
    }

    /**
     * Sync ALL calls from the phone log (merged with annotations) to the portal.
     * This ensures all calls appear on the portal, not just annotated ones.
     */
    fun syncAllCallsToPortal() {
        viewModelScope.launch {
            try {
                _syncStatus.value = "Sincronizzazione in corso..."
                val settings = ctx.loadMailSettings()
                Log.d("MainViewModel", "syncAllCalls: portalUrl='${settings.portalUrl}', apiKey='${settings.apiKey.take(8)}...'")
                if (settings.portalUrl.isBlank() || settings.apiKey.isBlank()) {
                    Log.e("MainViewModel", "syncAllCalls: SKIPPED - portalUrl or apiKey is blank!")
                    _syncStatus.value = "Errore: URL portale o API key mancante"
                    return@launch
                }

                // Get today's boundaries
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val dayStart = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, 1)
                val dayEnd = cal.timeInMillis
                val dateStr = PortalSync.formatDate(dayStart)

                // Get today's calls from phone log
                val todayCalls = CallLogReader.loadInDateRange(ctx, dayStart, dayEnd)
                Log.d("MainViewModel", "syncAllCalls: ${todayCalls.size} calls from phone log for $dateStr")

                if (todayCalls.isEmpty()) {
                    Log.d("MainViewModel", "syncAllCalls: no calls today, trying recent calls")
                    // Fallback: sync recent calls if no calls today
                    val recentCalls = _callLog.value
                    if (recentCalls.isEmpty()) {
                        _syncStatus.value = "Nessuna chiamata trovata oggi"
                        return@launch
                    }
                    // Group recent calls by date and sync each date
                    val callsByDate = recentCalls.groupBy { PortalSync.formatDate(it.date) }
                    var successCount = 0
                    for ((date, calls) in callsByDate) {
                        val dateCal = Calendar.getInstance().apply {
                            timeInMillis = calls.first().date
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val dStart = dateCal.timeInMillis
                        dateCal.add(Calendar.DAY_OF_YEAR, 1)
                        val dEnd = dateCal.timeInMillis
                        val notes = dao.getBetween(dStart, dEnd)
                        val ok = PortalSync.uploadAllCalls(
                            settings.portalUrl, settings.apiKey, date,
                            calls, notes
                        )
                        if (ok) successCount++
                    }
                    _syncStatus.value = "Sincronizzate $successCount/${callsByDate.size} giornate"
                    return@launch
                }

                // Get today's annotations from DB
                val todayAnnotations = dao.getBetween(dayStart, dayEnd)
                Log.d("MainViewModel", "syncAllCalls: ${todayAnnotations.size} annotations in DB")

                // Upload all calls merged with annotations
                val ok = PortalSync.uploadAllCalls(
                    settings.portalUrl, settings.apiKey, dateStr,
                    todayCalls, todayAnnotations
                )
                if (ok) {
                    Log.d("MainViewModel", "syncAllCalls OK: $dateStr (${todayCalls.size} calls)")
                    _syncStatus.value = "Sincronizzate ${todayCalls.size} chiamate per $dateStr"
                } else {
                    Log.e("MainViewModel", "syncAllCalls FAILED: $dateStr")
                    _syncStatus.value = "Errore sincronizzazione per $dateStr"
                }
            } catch (e: Throwable) {
                Log.e("MainViewModel", "syncAllCalls error", e)
                _syncStatus.value = "Errore: ${e.message ?: "errore sconosciuto"}"
            }
        }
    }

    /**
     * Start periodic sync every 5 minutes to keep portal up-to-date.
     */
    private fun startPeriodicSync() {
        viewModelScope.launch {
            while (true) {
                delay(5 * 60 * 1000L) // 5 minutes
                try {
                    // Refresh call log first
                    _callLog.value = CallLogReader.loadRecent(ctx, 100)
                    // Then sync all calls
                    syncAllCallsToPortalSilent()
                } catch (e: Throwable) {
                    Log.e("MainViewModel", "periodicSync error", e)
                }
            }
        }
    }

    /**
     * Silent version of syncAllCallsToPortal (no UI feedback, for background sync).
     */
    private suspend fun syncAllCallsToPortalSilent() {
        try {
            val settings = ctx.loadMailSettings()
            if (settings.portalUrl.isBlank() || settings.apiKey.isBlank()) return

            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dayStart = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val dayEnd = cal.timeInMillis
            val dateStr = PortalSync.formatDate(dayStart)

            val todayCalls = CallLogReader.loadInDateRange(ctx, dayStart, dayEnd)
            if (todayCalls.isEmpty()) return

            val todayAnnotations = dao.getBetween(dayStart, dayEnd)
            PortalSync.uploadAllCalls(
                settings.portalUrl, settings.apiKey, dateStr,
                todayCalls, todayAnnotations
            )
        } catch (e: Throwable) {
            Log.e("MainViewModel", "syncAllCallsSilent error", e)
        }
    }

    // --- Phone contacts (system) ---
    private val _contacts = MutableStateFlow<List<PhoneContact>>(emptyList())
    val contacts: StateFlow<List<PhoneContact>> = _contacts.asStateFlow()

    fun refreshContacts() {
        viewModelScope.launch {
            _contacts.value = ContactsReader.loadAll(ctx)
        }
    }

    // --- Client contacts (local DB with machines/serials) ---
    val clientContacts: StateFlow<List<ClientContact>> = contactDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun upsertClientContact(contact: ClientContact) {
        viewModelScope.launch { contactDao.upsert(contact) }
    }

    fun deleteClientContact(contact: ClientContact) {
        viewModelScope.launch { contactDao.delete(contact) }
    }

    fun findClientByPhone(phone: String, callback: (ClientContact?) -> Unit) {
        viewModelScope.launch {
            try {
                val stripped = phone.replace(Regex("[^0-9+]"), "")
                if (stripped.length < 4) {
                    callback(null)
                    return@launch
                }
                val suffix = stripped.takeLast(7)
                val all = contactDao.getAll()
                val match = all.find { client ->
                    val clientDigits = client.phone.replace(Regex("[^0-9+]"), "")
                    clientDigits.takeLast(7) == suffix
                }
                callback(match)
            } catch (_: Throwable) {
                callback(null)
            }
        }
    }

    // --- Mail settings ---
    private val _mailSettings = MutableStateFlow(ctx.loadMailSettings())
    val mailSettings: StateFlow<MailSettings> = _mailSettings.asStateFlow()

    fun updateMailSettings(settings: MailSettings) {
        _mailSettings.value = settings
        ctx.saveMailSettings(settings)
    }

    // --- Actions ---
    private val _snackbar = MutableStateFlow<String?>(null)
    val snackbar: StateFlow<String?> = _snackbar.asStateFlow()
    fun clearSnackbar() { _snackbar.value = null }

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun sendTestEmail() {
        viewModelScope.launch {
            _loading.value = true
            val result = MailSender.sendTestEmail(_mailSettings.value)
            _loading.value = false
            _snackbar.value = if (result.isSuccess) "Email di test inviata!" else "Errore: ${result.exceptionOrNull()?.message}"
        }
    }

    fun sendReport(notes: List<CallNote>) {
        viewModelScope.launch {
            _loading.value = true
            val (file, _) = ReportExporter.exportCsv(ctx, notes)
            val settings = _mailSettings.value
            val result = MailSender.sendReportEmail(
                settings,
                "Report VNAppCall",
                "Report giornaliero VNAppCall in allegato.",
                file
            )
            _loading.value = false
            _snackbar.value = if (result.isSuccess) "Report inviato via email!" else "Errore invio: ${result.exceptionOrNull()?.message}"
        }
    }

    fun syncToPortal(notes: List<CallNote>) {
        viewModelScope.launch {
            _loading.value = true
            try {
                // Re-read settings to pick up defaults for blank values
                val settings = ctx.loadMailSettings()
                _mailSettings.value = settings
                Log.d("MainViewModel", "syncToPortal: portalUrl='${settings.portalUrl}' apiKey='${settings.apiKey}' notes=${notes.size}")
                if (notes.isEmpty()) {
                    _snackbar.value = "Nessuna nota da sincronizzare"
                    return@launch
                }
                val dateStr = PortalSync.formatDate(notes.first().timestamp)
                Log.d("MainViewModel", "syncToPortal: dateStr=$dateStr")
                val ok = PortalSync.uploadReport(settings.portalUrl, settings.apiKey, dateStr, notes)
                Log.d("MainViewModel", "syncToPortal: result=$ok")
                _snackbar.value = if (ok) "Sincronizzato con il portale! (${notes.size} note inviate)" else "Errore sync portale: URL=${settings.portalUrl}"
            } catch (e: Throwable) {
                Log.e("MainViewModel", "syncToPortal failed", e)
                _snackbar.value = "Errore sync: ${e.message ?: "errore sconosciuto"}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun bulkSyncToPortal(fromMs: Long, toMs: Long) {
        viewModelScope.launch {
            _loading.value = true
            try {
                // Re-read settings to pick up defaults for blank values
                val settings = ctx.loadMailSettings()
                _mailSettings.value = settings
                Log.d("MainViewModel", "bulkSync: portalUrl='${settings.portalUrl}' apiKey='${settings.apiKey}'")
                val notes = dao.getBetween(fromMs, toMs)
                Log.d("MainViewModel", "bulkSync: found ${notes.size} notes from $fromMs to $toMs")
                if (notes.isEmpty()) {
                    _snackbar.value = "Nessuna nota da sincronizzare nel periodo"
                } else {
                    val byDate = notes.groupBy { PortalSync.formatDate(it.timestamp) }
                    val count = PortalSync.bulkSync(settings.portalUrl, settings.apiKey, byDate) { cur, total ->
                        Log.d("MainViewModel", "bulkSync progress: $cur/$total")
                    }
                    _snackbar.value = if (count > 0) "Sync completato: $count/${byDate.size} giorni inviati" else "Errore: nessun giorno sincronizzato su ${byDate.size}"
                }
            } catch (e: Throwable) {
                Log.e("MainViewModel", "bulkSync failed", e)
                _snackbar.value = "Errore sync: ${e.message ?: "errore sconosciuto"}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun findCallDuration(phone: String, timestamp: Long): Int {
        return CallLogReader.findDuration(ctx, phone, timestamp)
    }
}
