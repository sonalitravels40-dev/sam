package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ui.AppStrings
import com.example.ui.MainViewModel
import com.example.ui.screens.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppScaffold(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(viewModel: MainViewModel) {
    val context = LocalContext.current
    val isBengali by viewModel.isBengali.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isConsentAccepted by viewModel.isConsentAccepted.collectAsState()
    val isMonitoring by viewModel.isMonitoring.collectAsState()
    val selectedTransaction by viewModel.selectedTransaction.collectAsState()
    val previewTransaction by viewModel.previewTransaction.collectAsState()
    val feedbackMessage by viewModel.feedbackMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Launchers for minimum necessary SMS and Notification permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val smsGranted = permissions[Manifest.permission.RECEIVE_SMS] ?: false
        if (smsGranted) {
            // Permission granted
        }
    }

    LaunchedEffect(isConsentAccepted) {
        if (isConsentAccepted) {
            val neededPermissions = mutableListOf<String>()
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.RECEIVE_SMS)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    neededPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            if (neededPermissions.isNotEmpty()) {
                permissionLauncher.launch(neededPermissions.toTypedArray())
            }
        }
    }

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = FintechBlue,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CurrencyExchange,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = AppStrings.get("app_title", isBengali),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isMonitoring) StatusSuccess else StatusFailed)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isMonitoring) "Monitoring Active" else "Monitoring Inactive",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isMonitoring) StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Language switch button
                    FilledTonalButton(
                        onClick = { viewModel.toggleLanguage() },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isBengali) "English" else "বাংলা",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { viewModel.setTab(0) },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text(AppStrings.get("tab_dashboard", isBengali)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { viewModel.setTab(1) },
                    icon = { Icon(Icons.Default.Inbox, contentDescription = "Inbox") },
                    label = { Text(AppStrings.get("tab_inbox", isBengali)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { viewModel.setTab(2) },
                    icon = { Icon(Icons.Default.SettingsEthernet, contentDescription = "API") },
                    label = { Text(AppStrings.get("tab_settings", isBengali)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { viewModel.setTab(3) },
                    icon = { Icon(Icons.Default.Rule, contentDescription = "Senders") },
                    label = { Text(AppStrings.get("tab_senders", isBengali)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { viewModel.setTab(4) },
                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                    label = { Text(AppStrings.get("tab_history", isBengali)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 5,
                    onClick = { viewModel.setTab(5) },
                    icon = { Icon(Icons.Default.Policy, contentDescription = "Privacy") },
                    label = { Text(AppStrings.get("tab_privacy", isBengali)) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> DashboardScreen(
                    viewModel = viewModel,
                    context = context,
                    isBengali = isBengali,
                    onNavigateInbox = { viewModel.setTab(1) },
                    onNavigateSettings = { viewModel.setTab(2) }
                )
                1 -> InboxScreen(
                    viewModel = viewModel,
                    isBengali = isBengali,
                    onOpenDetails = { tx -> viewModel.selectTransaction(tx) },
                    onOpenPreview = { tx -> viewModel.openSendPreview(tx) }
                )
                2 -> ApiSettingsScreen(
                    viewModel = viewModel,
                    isBengali = isBengali
                )
                3 -> SenderParserScreen(
                    viewModel = viewModel,
                    isBengali = isBengali
                )
                4 -> HistoryScreen(
                    viewModel = viewModel,
                    isBengali = isBengali
                )
                5 -> PrivacyPolicyScreen(
                    viewModel = viewModel,
                    context = context,
                    isBengali = isBengali
                )
            }

            // Consent screen on first open
            if (!isConsentAccepted) {
                ConsentDialog(
                    isBengali = isBengali,
                    onAccept = {
                        viewModel.acceptConsent()
                    },
                    onDecline = {
                        // User can still see privacy policy
                        viewModel.setTab(5)
                    }
                )
            }

            // Bottom sheet for transaction details
            selectedTransaction?.let { tx ->
                TransactionDetailSheet(
                    transaction = tx,
                    viewModel = viewModel,
                    isBengali = isBengali,
                    onDismiss = { viewModel.selectTransaction(null) },
                    onSendToGateway = {
                        viewModel.selectTransaction(null)
                        viewModel.openSendPreview(tx)
                    }
                )
            }

            // Modal dialog for JSON Request Preview before sending
            previewTransaction?.let { tx ->
                RequestPreviewDialog(
                    transaction = tx,
                    viewModel = viewModel,
                    isBengali = isBengali,
                    onConfirm = { viewModel.confirmSendToGateway(tx) },
                    onDismiss = { viewModel.closeSendPreview() }
                )
            }
        }
    }
}
