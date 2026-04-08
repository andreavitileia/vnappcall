package com.vnsas.vnappcall

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.vnsas.vnappcall.data.loadMailSettings
import com.vnsas.vnappcall.ui.MainScreen
import com.vnsas.vnappcall.ui.theme.VNAppCallTheme
import com.vnsas.vnappcall.worker.DailyReportReceiver

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results[Manifest.permission.READ_CALL_LOG] == true) vm.refreshCallLog()
        if (results[Manifest.permission.READ_CONTACTS] == true) vm.refreshContacts()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        requestPermissions()
        scheduleAlarmIfNeeded()

        setContent {
            VNAppCallTheme {
                MainScreen(vm = vm)
            }
        }
    }

    private fun requestPermissions() {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG)
            != PackageManager.PERMISSION_GRANTED
        ) needed += Manifest.permission.READ_CALL_LOG
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) needed += Manifest.permission.READ_CONTACTS
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) needed += Manifest.permission.POST_NOTIFICATIONS

        if (needed.isNotEmpty()) {
            permLauncher.launch(needed.toTypedArray())
        } else {
            vm.refreshCallLog()
            vm.refreshContacts()
        }
    }

    private fun scheduleAlarmIfNeeded() {
        val settings = loadMailSettings()
        if (settings.autoEnabled) {
            DailyReportReceiver.scheduleDailyExact(this, settings.autoHour, settings.autoMinute)
        }
    }
}
