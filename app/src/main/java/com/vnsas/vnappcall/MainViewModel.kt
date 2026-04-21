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
     * Automatically sync all notes for a given date to the portal.
     * Called after every save/update/delete for real-time sync.
     */
    private suspend fun autoSyncDate(timestamp: Long) {
        try {
            val settings = ctx.loadMailSettings()
            if (settings.portalUrl.isBlank() || settings.apiKey.isBlank()) return

            val dateStr = PortalSync.formatDate(timestamp)
            // Get the day boundaries for this timestamp
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

            val dayNotes = dao.getBetween(dayStart, dayEnd)
            Log.d("MainViewModel", "autoSync: date=$dateStr, notes=${dayNotes.size}")

            if (dayNotes.isEmpty()) {
                // Send empty report to clear the date on the portal
                PortalSync.uploadReport(settings.portalUrl, settings.apiKey, dateStr, emptyList())
            } else {
                val ok = PortalSync.uploadReport(settings.portalUrl, settings.apiKey, dateStr, dayNotes)
                if (ok) {
                    Log.d("MainViewModel", "autoSync OK: $dateStr (${dayNotes.size} notes)")
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

    fun refreshCallLog() {
        viewModelScope.launch {
            _callLog.value = CallLogReader.loadRecent(ctx, 100)
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
