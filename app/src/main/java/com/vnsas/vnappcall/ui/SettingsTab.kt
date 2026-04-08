package com.vnsas.vnappcall.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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

    val fieldShape = RoundedCornerShape(12.dp)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            "Impostazioni",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        // SMTP Section
        SettingsSection("Server Email (SMTP)") {
            OutlinedTextField(
                value = host, onValueChange = { host = it },
                label = { Text("Host SMTP") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                shape = fieldShape, colors = fieldColors
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
                    singleLine = true, shape = fieldShape, colors = fieldColors
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("SSL", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(4.dp))
                    Switch(
                        checked = ssl, onCheckedChange = { ssl = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }
            OutlinedTextField(
                value = user, onValueChange = { user = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                shape = fieldShape, colors = fieldColors
            )
            OutlinedTextField(
                value = pass, onValueChange = { pass = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true, shape = fieldShape, colors = fieldColors
            )
            OutlinedTextField(
                value = from, onValueChange = { from = it },
                label = { Text("Mittente (email)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                shape = fieldShape, colors = fieldColors
            )
            OutlinedTextField(
                value = to, onValueChange = { to = it },
                label = { Text("Destinatario (email)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                shape = fieldShape, colors = fieldColors
            )
            OutlinedButton(
                onClick = { save(); vm.sendTestEmail() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Invia email di test") }
        }

        // Auto report
        SettingsSection("Report Automatico") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Invio automatico giornaliero", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = autoEnabled, onCheckedChange = { autoEnabled = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                )
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
                    singleLine = true, shape = fieldShape, colors = fieldColors
                )
                OutlinedTextField(
                    value = autoMinute, onValueChange = { autoMinute = it },
                    label = { Text("Minuto") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, shape = fieldShape, colors = fieldColors
                )
            }
        }

        // Portal
        SettingsSection("Portale Web") {
            OutlinedTextField(
                value = portalUrl, onValueChange = { portalUrl = it },
                label = { Text("URL Portale") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                shape = fieldShape, colors = fieldColors
            )
            OutlinedTextField(
                value = apiKey, onValueChange = { apiKey = it },
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                shape = fieldShape, colors = fieldColors
            )
        }

        // Save button
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = { save() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Save, null)
            Spacer(Modifier.width(8.dp))
            Text("Salva impostazioni")
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}
