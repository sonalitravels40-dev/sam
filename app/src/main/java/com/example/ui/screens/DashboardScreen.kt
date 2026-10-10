package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.SampleSmsData
import com.example.core.SampleSmsItem
import com.example.ui.AppStrings
import com.example.ui.MainViewModel
import com.example.ui.theme.*

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    context: Context,
    isBengali: Boolean,
    onNavigateInbox: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    val totalCount = viewModel.totalCount.value
    val sentCount = viewModel.sentCount.value
    val pendingCount = viewModel.pendingCount.value
    val failedCount = viewModel.failedCount.value
    val isMonitoring = viewModel.isMonitoring.value
    val lastSync = viewModel.lastSyncTime.value
    val apiSettings = viewModel.apiSettingsState.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Top Merchant Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = AppStrings.get("app_title", isBengali),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = AppStrings.get("tagline", isBengali),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        color = if (isMonitoring) StatusSuccessContainer else StatusFailedContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isMonitoring) StatusSuccess else StatusFailed)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isMonitoring)
                                    AppStrings.get("monitoring_active", isBengali)
                                else
                                    AppStrings.get("monitoring_stopped", isBengali),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isMonitoring) StatusSuccess else StatusFailed
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Monitoring Service Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.toggleMonitoring(context) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isMonitoring) StatusFailed else FintechBlue
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isMonitoring) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isMonitoring)
                                AppStrings.get("stop_monitoring", isBengali)
                            else
                                AppStrings.get("start_monitoring", isBengali),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    OutlinedButton(
                        onClick = onNavigateSettings,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isBengali) "এপিআই" else "Gateway")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Status row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${AppStrings.get("last_sync", isBengali)}: $lastSync",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "HTTPS: ${apiSettings.apiUrl.take(30)}...",
                        style = MaterialTheme.typography.labelSmall,
                        color = FintechBlueLight
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4 KPI Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KpiStatCard(
                modifier = Modifier.weight(1f),
                title = AppStrings.get("today_transactions", isBengali),
                value = totalCount.toString(),
                icon = Icons.Default.ReceiptLong,
                color = FintechBlue,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                onClick = onNavigateInbox
            )
            KpiStatCard(
                modifier = Modifier.weight(1f),
                title = AppStrings.get("successful_submissions", isBengali),
                value = sentCount.toString(),
                icon = Icons.Default.CheckCircle,
                color = StatusSuccess,
                containerColor = StatusSuccessContainer,
                onClick = onNavigateInbox
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KpiStatCard(
                modifier = Modifier.weight(1f),
                title = AppStrings.get("pending_review", isBengali),
                value = pendingCount.toString(),
                icon = Icons.Default.PendingActions,
                color = StatusPending,
                containerColor = StatusPendingContainer,
                onClick = onNavigateInbox
            )
            KpiStatCard(
                modifier = Modifier.weight(1f),
                title = AppStrings.get("failed_submissions", isBengali),
                value = failedCount.toString(),
                icon = Icons.Default.ErrorOutline,
                color = StatusFailed,
                containerColor = StatusFailedContainer,
                onClick = onNavigateInbox
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Sample SMS Testing Sandbox (English + Bengali)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = null,
                        tint = FintechBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = AppStrings.get("quick_test_banner", isBengali),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = AppStrings.get("quick_test_desc", isBengali),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(SampleSmsData.samples) { sample ->
                        SampleSmsChip(
                            sample = sample,
                            onSimulate = {
                                viewModel.simulateIncomingSms(sample)
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Safety & Policy Notice Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = StatusInfoContainer),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = StatusInfo,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isBengali) "নিরাপত্তা নীতি সক্রিয়" else "Zero-Trust Privacy Active",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = StatusInfo
                    )
                    Text(
                        text = if (isBengali)
                            "ওটিপি ও গোপন পিন স্বয়ংক্রিয়ভাবে ব্লক হয়। কোনো লেনদেন আপনার অনুমতি ছাড়া গেটওয়েতে পাঠানো হয় না।"
                        else
                            "OTPs and PINs are automatically blocked. No transaction is uploaded without manual merchant confirmation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun KpiStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    containerColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SampleSmsChip(
    sample: SampleSmsItem,
    onSimulate: () -> Unit
) {
    Surface(
        onClick = onSimulate,
        shape = RoundedCornerShape(12.dp),
        color = if (sample.isSensitiveTest) StatusFailedContainer else MaterialTheme.colorScheme.surfaceVariant,
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .width(180.dp)
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = sample.provider,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (sample.isSensitiveTest) StatusFailed else FintechBlue
                )
                if (sample.isSensitiveTest) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = "Sensitive Test",
                        tint = StatusFailed,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = sample.title,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Simulate SMS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
