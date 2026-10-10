package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.core.ApiSettings
import com.example.ui.AppStrings
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiSettingsScreen(
    viewModel: MainViewModel,
    isBengali: Boolean
) {
    val currentSettings = viewModel.apiSettingsState.value
    val currentSecret = viewModel.apiSecretState.value
    val isSending = viewModel.isSending.value
    val testResult = viewModel.testApiResult.value

    var urlInput by remember { mutableStateOf(currentSettings.apiUrl) }
    var authTypeInput by remember { mutableStateOf(currentSettings.authType) }
    var headerNameInput by remember { mutableStateOf(currentSettings.authHeaderName) }
    var secretInput by remember { mutableStateOf(currentSecret) }
    var isSecretVisible by remember { mutableStateOf(false) }
    var merchantIdInput by remember { mutableStateOf(currentSettings.merchantId) }
    var deviceIdInput by remember { mutableStateOf(currentSettings.deviceId) }
    var timeoutInput by remember { mutableStateOf(currentSettings.timeoutSeconds.toString()) }
    var preventDuplicatesInput by remember { mutableStateOf(currentSettings.preventDuplicates) }

    var showConfirmSaveDialog by remember { mutableStateOf(false) }

    val calculatedDomain = remember(urlInput) {
        try {
            URL(urlInput).host
        } catch (_: Exception) {
            "invalid domain"
        }
    }

    val isHttpsValid = urlInput.startsWith("https://", ignoreCase = true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = AppStrings.get("api_settings_title", isBengali),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Security Notice Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.VpnKey,
                    contentDescription = null,
                    tint = FintechBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = AppStrings.get("secret_encrypted_badge", isBengali),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = FintechBlue
                    )
                    Text(
                        text = "Hardware-backed hardware storage. Secrets are never logged or stored in plain text.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // HTTPS URL Field
        OutlinedTextField(
            value = urlInput,
            onValueChange = { urlInput = it.trim() },
            label = { Text(AppStrings.get("api_url_label", isBengali)) },
            modifier = Modifier.fillMaxWidth(),
            isError = !isHttpsValid && urlInput.isNotBlank(),
            supportingText = {
                if (!isHttpsValid && urlInput.isNotBlank()) {
                    Text(
                        text = "HTTPS Required! Insecure HTTP endpoints are rejected for merchant safety.",
                        color = StatusFailed
                    )
                } else {
                    Text("${AppStrings.get("endpoint_domain", isBengali)}: $calculatedDomain")
                }
            },
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Auth Scheme Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = authTypeInput == "Bearer",
                onClick = {
                    authTypeInput = "Bearer"
                    headerNameInput = "Authorization"
                },
                label = { Text("Bearer Token") },
                shape = RoundedCornerShape(10.dp)
            )

            FilterChip(
                selected = authTypeInput == "API Key",
                onClick = {
                    authTypeInput = "API Key"
                    headerNameInput = "X-API-KEY"
                },
                label = { Text("API Key Header") },
                shape = RoundedCornerShape(10.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Header Name
        OutlinedTextField(
            value = headerNameInput,
            onValueChange = { headerNameInput = it },
            label = { Text(AppStrings.get("auth_header_label", isBengali)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Secret / Bearer Token Value
        OutlinedTextField(
            value = secretInput,
            onValueChange = { secretInput = it },
            label = { Text(AppStrings.get("secret_value_label", isBengali)) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (isSecretVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { isSecretVisible = !isSecretVisible }) {
                    Icon(
                        imageVector = if (isSecretVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle Secret"
                    )
                }
            },
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Merchant ID & Device ID Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = merchantIdInput,
                onValueChange = { merchantIdInput = it },
                label = { Text(AppStrings.get("merchant_id_label", isBengali)) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = deviceIdInput,
                onValueChange = { deviceIdInput = it },
                label = { Text(AppStrings.get("device_id_label", isBengali)) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Timeout Field
        OutlinedTextField(
            value = timeoutInput,
            onValueChange = { timeoutInput = it.filter { ch -> ch.isDigit() } },
            label = { Text(AppStrings.get("timeout_label", isBengali)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Duplicate Prevention Setting Switch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = AppStrings.get("prevent_duplicates_label", isBengali),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Detects previously received transaction IDs and prevents duplicate gateway calls.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = preventDuplicatesInput,
                onCheckedChange = { preventDuplicatesInput = it }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action Buttons: Save & Test API
        Button(
            onClick = {
                if (urlInput != currentSettings.apiUrl) {
                    showConfirmSaveDialog = true
                } else {
                    val newSettings = ApiSettings(
                        apiUrl = urlInput,
                        authType = authTypeInput,
                        authHeaderName = headerNameInput,
                        merchantId = merchantIdInput,
                        deviceId = deviceIdInput,
                        timeoutSeconds = timeoutInput.toIntOrNull() ?: 15,
                        preventDuplicates = preventDuplicatesInput
                    )
                    viewModel.saveApiSettings(newSettings, secretInput)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = isHttpsValid,
            colors = ButtonDefaults.buttonColors(containerColor = FintechBlue),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(AppStrings.get("save_settings_button", isBengali), fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = { viewModel.testApiEndpoint() },
            modifier = Modifier.fillMaxWidth(),
            enabled = isHttpsValid && !isSending,
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isSending) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(6.dp))
            } else {
                Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(AppStrings.get("test_api_button", isBengali))
        }

        // Test API Result Banner if available
        if (testResult != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (testResult.isSuccess) StatusSuccessContainer else StatusFailedContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (testResult.isSuccess) "Test Passed (HTTP ${testResult.httpCode})" else "Test Failed (HTTP ${testResult.httpCode})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (testResult.isSuccess) StatusSuccess else StatusFailed
                    )
                    Text(
                        text = "Target Domain: ${testResult.destinationDomain}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (testResult.errorMessage != null) {
                        Text(
                            text = testResult.errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusFailed
                        )
                    }
                }
            }
        }
    }

    // Confirmation dialog before saving new endpoint domain
    if (showConfirmSaveDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmSaveDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = StatusPending) },
            title = { Text(if (isBengali) "নতুন এন্ডপয়েন্ট নিশ্চিতকরণ" else "Confirm Endpoint Change") },
            text = {
                Text(
                    if (isBengali)
                        "আপনি এপিআই গেটওয়ে ডোমেইন পরিবর্তন করছেন: $calculatedDomain। আপনি কি নিশ্চিত?"
                    else
                        "You are updating the target reconciliation gateway to domain: $calculatedDomain. Please confirm this is your authorized merchant server."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newSettings = ApiSettings(
                            apiUrl = urlInput,
                            authType = authTypeInput,
                            authHeaderName = headerNameInput,
                            merchantId = merchantIdInput,
                            deviceId = deviceIdInput,
                            timeoutSeconds = timeoutInput.toIntOrNull() ?: 15,
                            preventDuplicates = preventDuplicatesInput
                        )
                        viewModel.saveApiSettings(newSettings, secretInput)
                        showConfirmSaveDialog = false
                    }
                ) {
                    Text(if (isBengali) "নিশ্চিত" else "Confirm & Save")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmSaveDialog = false }) {
                    Text(AppStrings.get("cancel", isBengali))
                }
            }
        )
    }
}
