package com.vnsas.vnappcall.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.vnsas.vnappcall.MainViewModel
import com.vnsas.vnappcall.worker.DailyReportReceiver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTab(vm: MainViewModel) {
    val settings by vm.mailSettings.collectAsState()
    val context = LocalContext.current

    var host by remember(settings) { mutableStateOf(settings.host) }
    var port by remember(settings) { mutableStateOf(settings.port) }
    var ssl by remember(settings) { mutableStateOf(settings.ssl) }
    var user by remember(settings) { mutableStateOf(settings.user) }
    var pass by remember(settings) { mutableStateOf(settings.pass) }
    var from by remember(settings) { mutableStateOf(settings.from) }
    var to by remember(settings) { mutableStateOf(settings.to) }
    var autoEnabled by remember(settings) { mutableStateOf(settings.autoEnabled) }
    var autoHour by remember(settings) { mutableStateOf(settings.autoHour.toString()) }
    var autoMinute by remember(settings) { mutableStateOf(settings.autoMinute.toString()) }
    var portalUrl by remember(settings) { mutableStateOf(settings.portalUrl) }
    var apiKey by remember(settings) { mutableStateOf(settings.apiKey) }

    fun save() {
        val updated = settings.copy(
            host = host, port = port, ssl = ssl,
            user = user, pass = pass, from = from, to = to,
            autoEnabled = autoEnabled,
            autoHour = autoHour.toIntOrNull() ?: 8,
            autoMinute = autoMinute.toIntOrNull() ?: 0,
            portalUrl = portalUrl, apiKey = apiKey
        )
        vm.updateMailSettings(updated)
        if (updated.autoEnabled) {
            DailyReportReceiver.scheduleDailyExact(context, updated.autoHour, updated.autoMinute)
        } else {
            DailyReportReceiver.cancel(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Impostazioni") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // SMTP Section
            SectionTitle("Server Email (SMTP)")

            OutlinedTextField(
                value = host, onValueChange = { host = it },
                label = { Text("Host SMTP") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = port, onValueChange = { port = it },
                    label = { Text("Porta") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("SSL")
                    Spacer(Modifier.padding(start = 4.dp))
                    Switch(checked = ssl, onCheckedChange = { ssl = it })
                }
            }
            OutlinedTextField(
                value = user, onValueChange = { user = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = pass, onValueChange = { pass = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )
            OutlinedTextField(
                value = from, onValueChange = { from = it },
                label = { Text("Mittente (email)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = to, onValueChange = { to = it },
                label = { Text("Destinatario (email)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedButton(
                onClick = {
                    save()
                    vm.sendTestEmail()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Invia email di test")
            }

            Divider(modifier = Modifier.padding(vertical = 4.dp))

            // Auto report
            SectionTitle("Report Automatico")

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Invio automatico giornaliero")
                Switch(checked = autoEnabled, onCheckedChange = { autoEnabled = it })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = autoHour, onValueChange = { autoHour = it },
                    label = { Text("Ora") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = autoMinute, onValueChange = { autoMinute = it },
                    label = { Text("Minuto") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            Divider(modifier = Modifier.padding(vertical = 4.dp))

            // Portal
            SectionTitle("Portale Web")

            OutlinedTextField(
                value = portalUrl, onValueChange = { portalUrl = it },
                label = { Text("URL Portale") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = apiKey, onValueChange = { apiKey = it },
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { save() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Salva impostazioni")
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
}
