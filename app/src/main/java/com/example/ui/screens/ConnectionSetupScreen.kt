package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.core.connection.ConnectionTestResult
import com.example.core.connection.ServerConnectionManager
import com.example.core.connection.ServerConnectionMode
import com.example.core.connection.ServerConnectionStore
import com.example.core.security.SecretVault
import com.example.ui.i18n.AppUiStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ConnectionSetupScreen(appLanguage: String? = null) {
    val uiLang = appLanguage
    val context = LocalContext.current
    val store = remember { ServerConnectionStore(context) }
    val manager = remember { ServerConnectionManager(store, SecretVault(context)) }
    val scope = rememberCoroutineScope()

    var mode by remember { mutableStateOf(store.getMode()) }
    var customUrl by remember { mutableStateOf(store.getCustomBaseUrl()) }
    var busy by remember { mutableStateOf(false) }
    var lastResult by remember { mutableStateOf<ConnectionTestResult?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = AppUiStrings.connectionSetupTitle(uiLang),
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = AppUiStrings.connectionSetupSubtitle(uiLang),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = mode == ServerConnectionMode.CENTRAL,
                onClick = { mode = ServerConnectionMode.CENTRAL },
                label = { Text(AppUiStrings.connectionModeCentral(uiLang)) }
            )
            FilterChip(
                selected = mode == ServerConnectionMode.CUSTOM,
                onClick = { mode = ServerConnectionMode.CUSTOM },
                label = { Text(AppUiStrings.connectionModeCustom(uiLang)) }
            )
        }

        if (mode == ServerConnectionMode.CENTRAL) {
            Text(
                text = AppUiStrings.connectionCentralUrlHint(uiLang, store.getCentralBaseUrl().ifBlank { "—" }),
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            OutlinedTextField(
                value = customUrl,
                onValueChange = { customUrl = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(AppUiStrings.connectionCustomUrlLabel(uiLang)) },
                placeholder = { Text("https://api.example.com") }
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    busy = true
                    statusMessage = null
                    scope.launch {
                        val url = if (mode == ServerConnectionMode.CENTRAL) store.getCentralBaseUrl() else customUrl
                        val result = withContext(Dispatchers.IO) {
                            manager.testUrl(url, BuildConfig.DEBUG)
                        }
                        lastResult = result
                        statusMessage = if (result.ok) {
                            AppUiStrings.connectionTestOk(uiLang)
                        } else {
                            AppUiStrings.connectionTestFailed(uiLang, result.errorCode, result.errorMessage)
                        }
                        busy = false
                    }
                },
                enabled = !busy,
                modifier = Modifier.weight(1f)
            ) {
                if (busy) CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                else Text(AppUiStrings.connectionTestButton(uiLang))
            }
            Button(
                onClick = {
                    val prior = lastResult
                    if (prior == null || !prior.ok || prior.discovery == null) {
                        statusMessage = AppUiStrings.connectionMustTestFirst(uiLang)
                        return@Button
                    }
                    busy = true
                    scope.launch {
                        val applyResult = withContext(Dispatchers.IO) {
                            manager.applyConnection(mode, customUrl, prior)
                        }
                        statusMessage = if (applyResult.isSuccess) {
                            AppUiStrings.connectionSaved(uiLang)
                        } else {
                            applyResult.exceptionOrNull()?.message
                                ?: AppUiStrings.connectionTestFailed(uiLang, "APPLY_FAILED", null)
                        }
                        busy = false
                    }
                },
                enabled = !busy,
                modifier = Modifier.weight(1f)
            ) {
                Text(AppUiStrings.connectionSaveButton(uiLang))
            }
        }

        OutlinedButton(
            onClick = {
                manager.disconnect()
                mode = ServerConnectionMode.CENTRAL
                lastResult = null
                statusMessage = AppUiStrings.connectionDisconnected(uiLang)
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(AppUiStrings.connectionDisconnectButton(uiLang))
        }

        statusMessage?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium)
        }

        lastResult?.let { result ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (result.ok) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(AppUiStrings.connectionStatusTitle(uiLang), style = MaterialTheme.typography.titleSmall)
                    Text("${AppUiStrings.connectionFieldUrl(uiLang)}: ${result.baseUrl}")
                    result.discovery?.let { d ->
                        Text("${AppUiStrings.connectionFieldService(uiLang)}: ${d.service}")
                        Text("${AppUiStrings.connectionFieldVersion(uiLang)}: ${d.apiVersion}")
                        Text("${AppUiStrings.connectionFieldProtocol(uiLang)}: ${d.protocol}")
                        Text("${AppUiStrings.connectionFieldCapabilities(uiLang)}: ${d.capabilities.joinToString()}")
                        Text("${AppUiStrings.connectionFieldOauthClient(uiLang)}: ${d.googleClientId.take(24)}…")
                    }
                    result.errorCode?.let {
                        Text("${AppUiStrings.connectionFieldError(uiLang)}: $it")
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = AppUiStrings.connectionSecurityNote(uiLang),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
