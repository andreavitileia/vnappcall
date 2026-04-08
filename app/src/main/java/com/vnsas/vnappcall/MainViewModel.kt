package com.vnsas.vnappcall

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vnsas.vnappcall.data.CallNote
import com.vnsas.vnappcall.data.CallNoteDao
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
        viewModelScope.launch { dao.upsert(note) }
    }

    fun deleteNote(note: CallNote) {
        viewModelScope.launch { dao.delete(note) }
    }

    // --- Call log ---
    private val _callLog = MutableStateFlow<List<CallLogEntry>>(emptyList())
    val callLog: StateFlow<List<CallLogEntry>> = _callLog.asStateFlow()

    fun refreshCallLog() {
        viewModelScope.launch {
            _callLog.value = CallLogReader.loadRecent(ctx, 100)
        }
    }

    // --- Contacts ---
    private val _contacts = MutableStateFlow<List<PhoneContact>>(emptyList())
    val contacts: StateFlow<List<PhoneContact>> = _contacts.asStateFlow()

    fun refreshContacts() {
        viewModelScope.launch {
            _contacts.value = ContactsReader.loadAll(ctx)
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
            val settings = _mailSettings.value
            val dateStr = if (notes.isNotEmpty()) PortalSync.formatDate(notes.first().timestamp) else PortalSync.formatDate(System.currentTimeMillis())
            val ok = PortalSync.uploadReport(settings.portalUrl, settings.apiKey, dateStr, notes)
            _loading.value = false
            _snackbar.value = if (ok) "Sincronizzato con il portale!" else "Errore sincronizzazione portale"
        }
    }

    fun bulkSyncToPortal(fromMs: Long, toMs: Long) {
        viewModelScope.launch {
            _loading.value = true
            val settings = _mailSettings.value
            val notes = dao.getBetween(fromMs, toMs)
            val byDate = notes.groupBy { PortalSync.formatDate(it.timestamp) }
            val count = PortalSync.bulkSync(settings.portalUrl, settings.apiKey, byDate) { _, _ -> }
            _loading.value = false
            _snackbar.value = "Sync completato: $count/${byDate.size} giorni inviati"
        }
    }

    fun findCallDuration(phone: String, timestamp: Long): Int {
        return CallLogReader.findDuration(ctx, phone, timestamp)
    }
}
