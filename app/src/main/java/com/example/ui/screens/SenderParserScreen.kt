package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.SampleSmsData
import com.example.data.ApprovedSenderEntity
import com.example.ui.AppStrings
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SenderParserScreen(
    viewModel: MainViewModel,
    isBengali: Boolean
) {
    val senders = viewModel.approvedSenders.value
    val parserSender = viewModel.parserSender.value
    val parserInput = viewModel.parserInput.value
    val parserResult = viewModel.parserResult.value

    var showAddSenderDialog by remember { mutableStateOf(false) }
    var newSenderPattern by remember { mutableStateOf("") }
    var newProviderName by remember { mutableStateOf("") }
    var newNotes by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Section 1: Approved Sender IDs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = AppStrings.get("approved_senders_title", isBengali),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isBengali)
                        "শুধুমাত্র এই প্রেরকদের এসএমএস অনুমোদিত ও প্রসেস করা হবে।"
                    else
                        "Only SMS originating from these verified IDs will be processed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = { showAddSenderDialog = true }) {
                Icon(
                    imageVector = Icons.Default.AddCircle,
                    contentDescription = "Add Sender",
                    tint = FintechBlue,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Sender List Cards
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            senders.forEach { sender ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (sender.isEnabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = sender.providerName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (sender.isEnabled) FintechBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = sender.senderPattern,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (sender.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = sender.notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = sender.isEnabled,
                                onCheckedChange = { viewModel.toggleApprovedSender(sender) }
                            )
                            IconButton(onClick = { viewModel.deleteApprovedSender(sender) }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = StatusFailed.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Section 2: Live SMS Parser Sandbox
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = AppStrings.get("parser_tester_title", isBengali),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = AppStrings.get("parser_tester_desc", isBengali),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Preset Sample Selectors
        Text(
            text = "Load Sample SMS (English / Bengali / Safety Test):",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(SampleSmsData.samples) { sample ->
                SuggestionChip(
                    onClick = { viewModel.loadSampleIntoParser(sample) },
                    label = { Text(sample.title) },
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Sender input
        OutlinedTextField(
            value = parserSender,
            onValueChange = { viewModel.setParserSender(it) },
            label = { Text("Sender ID") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // SMS Body input
        OutlinedTextField(
            value = parserInput,
            onValueChange = { viewModel.setParserInput(it) },
            label = { Text("SMS Message Body") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Parsed Live Result Display Card
        if (parserResult != null) {
            val res = parserResult
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (res.isValid) MaterialTheme.colorScheme.surfaceVariant else StatusFailedContainer
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (res.isValid) "Parsing Result: Valid Payment" else "Parsing Result: Rejected / Incomplete",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (res.isValid) StatusSuccess else StatusFailed
                        )
                        Surface(
                            color = if (res.confidenceScore >= 80) StatusSuccessContainer else StatusPendingContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Confidence: ${res.confidenceScore}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (res.confidenceScore >= 80) StatusSuccess else StatusPending,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (res.rejectReason != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = res.rejectReason,
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusFailed
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Provider: ${res.provider}", style = MaterialTheme.typography.bodySmall)
                            Text(text = "Txn ID: ${res.transactionId}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text(text = "Customer Ref: ${res.customerReference.ifBlank { "N/A" }}", style = MaterialTheme.typography.bodySmall)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Amount: Tk ${String.format(Locale.US, "%.2f", res.amount)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = FintechBlue
                            )
                            if (res.balance != null) {
                                Text(
                                    text = "Balance: Tk ${String.format(Locale.US, "%.2f", res.balance)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Sender Dialog
    if (showAddSenderDialog) {
        AlertDialog(
            onDismissRequest = { showAddSenderDialog = false },
            title = { Text(AppStrings.get("add_sender", isBengali)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newProviderName,
                        onValueChange = { newProviderName = it },
                        label = { Text("Provider Name (e.g. bKash)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newSenderPattern,
                        onValueChange = { newSenderPattern = it },
                        label = { Text("Sender Pattern / ID (e.g. bKash or 16216)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newNotes,
                        onValueChange = { newNotes = it },
                        label = { Text("Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newSenderPattern.isNotBlank() && newProviderName.isNotBlank()) {
                            viewModel.addApprovedSender(newSenderPattern, newProviderName, newNotes)
                            showAddSenderDialog = false
                            newSenderPattern = ""
                            newProviderName = ""
                            newNotes = ""
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddSenderDialog = false }) {
                    Text(AppStrings.get("cancel", isBengali))
                }
            }
        )
    }
}
