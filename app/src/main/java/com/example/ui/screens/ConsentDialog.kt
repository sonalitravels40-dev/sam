package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.StatusSuccess

@Composable
fun ConsentDialog(
    isBengali: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Require explicit choice */ },
        icon = {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = if (isBengali) "অনুমতি ও গোপনীয়তা সম্মতি" else "Merchant SMS Reading Consent",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (isBengali)
                        "SMS READER X শুধুমাত্র আপনার অনুমোদিত মার্চেন্ট ডিভাইসে পেমেন্ট বিজ্ঞপ্তি পড়ার জন্য ডিজাইন করা হয়েছে। চালিয়ে যেতে অনুগ্রহ করে শর্তাবলী পর্যালোচনা করুন:"
                    else
                        "SMS READER X is built strictly for authorized merchant devices to automate payment reconciliation. Please review our data protection practices:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                ConsentBullet(
                    icon = Icons.Default.CheckCircle,
                    title = if (isBengali) "শুধুমাত্র অনুমোদিত পেমেন্ট এসএমএস" else "Approved Senders Only",
                    desc = if (isBengali)
                        "শুধুমাত্র বিকাশ, নগদ, রকেট এবং উপায়ের মতো কনফিগার করা সেন্ডারদের লেনদেন পড়া হবে।"
                    else
                        "Processes payment notifications exclusively from user-approved sender IDs (bKash, Nagad, Rocket, Upay)."
                )

                Spacer(modifier = Modifier.height(12.dp))

                ConsentBullet(
                    icon = Icons.Default.Lock,
                    title = if (isBengali) "শূন্য ওটিপি ও গোপন পিন ট্র্যাকিং" else "Zero OTP / PIN / Authentication Transmission",
                    desc = if (isBengali)
                        "ওটিপি, গোপন পিন, পাসওয়ার্ড বা ব্যক্তিগত এসএমএস কখনোই পাঠানো বা সংরক্ষণ করা হবে না।"
                    else
                        "Strictly blocks and discards all OTPs, PINs, passwords, login codes, card details, or authentication texts."
                )

                Spacer(modifier = Modifier.height(12.dp))

                ConsentBullet(
                    icon = Icons.Default.Notifications,
                    title = if (isBengali) "ম্যানুয়াল রিভিউ ও দৃশ্যমান নোটিফিকেশন" else "Manual Review & Visible Monitoring",
                    desc = if (isBengali)
                        "প্রতিটি লেনদেন আপনার অনুমোদনের পর এনক্রিপ্টকৃত HTTPS গেটওয়েতে প্রেরিত হয়। যে কোনো সময় মনিটরিং বন্ধ করতে পারবেন।"
                    else
                        "Transactions remain in Pending Review until you tap 'Send to Gateway'. Monitoring displays a visible background notification and can be stopped instantly."
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(if (isBengali) "সম্মতি দিচ্ছি ও এগিয়ে যান" else "I Consent & Agree")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDecline) {
                Text(if (isBengali) "বাতিল" else "Decline")
            }
        }
    )
}

@Composable
private fun ConsentBullet(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = StatusSuccess,
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
