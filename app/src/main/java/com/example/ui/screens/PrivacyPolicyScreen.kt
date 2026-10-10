package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.ui.AppStrings
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PrivacyPolicyScreen(
    viewModel: MainViewModel,
    context: Context,
    isBengali: Boolean
) {
    val auditLogs = viewModel.auditLogs.value
    var showWipeConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = AppStrings.get("privacy_title", isBengali),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 1. Google Play SMS Policy Compliance Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = StatusSuccess,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isBengali) "গুগল প্লে এসএমএস পলিসি অনুবর্তিতা" else "Google Play Policy Compliance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                PolicySectionItem(
                    title = if (isBengali) "উদ্দেশ্য ও সীমিত পরিধি" else "Authorized Merchant Use Case",
                    content = if (isBengali)
                        "এই অ্যাপটি শুধুমাত্র বাণিজ্যিক মার্চেন্টদের নিজস্ব ডিভাইসে আর্থিক লেনদেন স্বয়ংক্রিয়ভাবে সমন্বয়ের জন্য তৈরি। সাধারণ চ্যাট বা ব্যক্তিগত যোগাযোগের জন্য এটি নয়।"
                    else
                        "SMS READER X operates strictly on merchant devices for authorized financial transaction reconciliation. It is never used for personal messaging surveillance."
                )

                Spacer(modifier = Modifier.height(8.dp))

                PolicySectionItem(
                    title = if (isBengali) "প্রয়োজনীয় অনুমতিসমূহ (Permissions)" else "Mandatory Android Permissions",
                    content = "• RECEIVE_SMS: Used strictly to detect payment notification broadcasts from approved financial sender IDs.\n• POST_NOTIFICATIONS: Notifies merchants of pending payments requiring review without exposing sensitive data.\n• FOREGROUND_SERVICE: Displays persistent notification so the merchant is always visibly aware that monitoring is running."
                )

                Spacer(modifier = Modifier.height(8.dp))

                PolicySectionItem(
                    title = if (isBengali) "কী কী ডেটা পড়া ও পাঠানো হয় না" else "Prohibited Data Handling",
                    content = "• OTPs, PINs, passwords, authentication tokens, and personal texts are strictly blocked by safety filters.\n• Raw SMS messages are never transmitted in full.\n• API secrets are encrypted using hardware-backed Android Keystore."
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Data Deletion & Privacy Rights Card (Mandatory for Google Play)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = StatusFailed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = AppStrings.get("data_deletion_title", isBengali),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isBengali)
                        "গুগল প্লে স্টোর নীতি অনুযায়ী, ব্যবহারকারী যে কোনো সময় তাদের সমস্ত সংরক্ষিত লেনদেন, এপিআই কি এবং অডিট লগ তাৎক্ষণিকভাবে ডিলিট করতে পারবেন।"
                    else
                        "Under Google Play Developer Policy, merchants have full rights to wipe all locally stored transaction records, cached history, and Keystore secrets at any time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.exportTransactionsCsv { file ->
                                // Trigger share
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/csv"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Export Report CSV"))
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FintechBlue)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(AppStrings.get("export_csv", isBengali))
                    }

                    OutlinedButton(
                        onClick = { showWipeConfirmDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusFailed)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isBengali) "সব ডেটা মুছুন" else "Wipe All")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Testing Checklist & Limitations Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Checklist,
                        contentDescription = null,
                        tint = FintechBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isBengali) "টেস্টিং চেকলিস্ট ও সতর্কতা" else "Merchant Verification Checklist",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                ChecklistItem("✓ Grant SMS & Notification permissions on prompt.")
                ChecklistItem("✓ Configure your company's HTTPS reconciliation endpoint.")
                ChecklistItem("✓ Test bKash, Nagad, Rocket, Upay using the built-in Sandbox.")
                ChecklistItem("✓ Test OTP safety filter to ensure authentication codes are rejected.")
                ChecklistItem("✓ Verify transactions in Pending list before sending.")
                ChecklistItem("✓ Stop monitoring when device is off business hours.")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Local Audit Logs
        Text(
            text = AppStrings.get("audit_logs_title", isBengali),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))

        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            auditLogs.take(15).forEach { log ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dateFormat.format(Date(log.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = log.action,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = FintechBlue
                            )
                            Text(
                                text = log.details,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }

    // Confirmation dialog before wiping all data
    if (showWipeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showWipeConfirmDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = StatusFailed) },
            title = { Text(if (isBengali) "সমস্ত ডেটা মুছে ফেলার নিশ্চিতকরণ" else "Wipe All Merchant Data?") },
            text = {
                Text(
                    if (isBengali)
                        "এই অ্যাকশনটি সমস্ত স্থানীয় লেনদেন, সংরক্ষিত এপিআই কি, সাবমিশন হিস্ট্রি এবং অডিট লগ স্থায়ীভাবে মুছে ফেলবে। এটি পুনরুদ্ধারযোগ্য নয়।"
                    else
                        "This will permanently delete all local transactions, Keystore encrypted API secrets, history logs, and reset the application. This action cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.wipeAllData()
                        showWipeConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusFailed)
                ) {
                    Text(if (isBengali) "হ্যাঁ, সব মুছুন" else "Yes, Wipe Everything")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showWipeConfirmDialog = false }) {
                    Text(AppStrings.get("cancel", isBengali))
                }
            }
        )
    }
}

@Composable
private fun PolicySectionItem(title: String, content: String) {
    Column {
        Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ChecklistItem(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}
