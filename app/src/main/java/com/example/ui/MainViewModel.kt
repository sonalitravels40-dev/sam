package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.ApiResult
import com.example.core.ApiSettings
import com.example.core.ParsedTransactionResult
import com.example.core.PaymentSmsProcessor
import com.example.core.SampleSmsData
import com.example.core.SampleSmsItem
import com.example.data.AppDatabase
import com.example.data.ApprovedSenderEntity
import com.example.data.AuditLogEntity
import com.example.data.SettingsRepository
import com.example.data.SubmissionHistoryEntity
import com.example.data.TransactionEntity
import com.example.data.TransactionRepository
import com.example.service.SmsMonitoringService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val transactionRepo = TransactionRepository(application, db)
    private val settingsRepo = SettingsRepository(application)

    // Reactive database flows
    val allTransactions: StateFlow<List<TransactionEntity>> = transactionRepo.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingCount: StateFlow<Int> = transactionRepo.pendingCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val sentCount: StateFlow<Int> = transactionRepo.sentCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val failedCount: StateFlow<Int> = transactionRepo.failedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalCount: StateFlow<Int> = transactionRepo.totalCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val submissionHistory: StateFlow<List<SubmissionHistoryEntity>> = transactionRepo.submissionHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val approvedSenders: StateFlow<List<ApprovedSenderEntity>> = transactionRepo.approvedSenders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLogEntity>> = transactionRepo.auditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI state
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _filterStatus = MutableStateFlow("ALL")
    val filterStatus: StateFlow<String> = _filterStatus.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isBengali = MutableStateFlow(settingsRepo.getLanguage() == "bn")
    val isBengali: StateFlow<Boolean> = _isBengali.asStateFlow()

    private val _isMonitoring = MutableStateFlow(SmsMonitoringService.isMonitoringActive(application))
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    private val _isConsentAccepted = MutableStateFlow(settingsRepo.isConsentAccepted())
    val isConsentAccepted: StateFlow<Boolean> = _isConsentAccepted.asStateFlow()

    private val _selectedTransaction = MutableStateFlow<TransactionEntity?>(null)
    val selectedTransaction: StateFlow<TransactionEntity?> = _selectedTransaction.asStateFlow()

    private val _previewTransaction = MutableStateFlow<TransactionEntity?>(null)
    val previewTransaction: StateFlow<TransactionEntity?> = _previewTransaction.asStateFlow()

    private val _apiSettingsState = MutableStateFlow(settingsRepo.getApiSettings())
    val apiSettingsState: StateFlow<ApiSettings> = _apiSettingsState.asStateFlow()

    private val _apiSecretState = MutableStateFlow(settingsRepo.getApiSecret())
    val apiSecretState: StateFlow<String> = _apiSecretState.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _lastSyncTime = MutableStateFlow("Never")
    val lastSyncTime: StateFlow<String> = _lastSyncTime.asStateFlow()

    private val _testApiResult = MutableStateFlow<ApiResult?>(null)
    val testApiResult: StateFlow<ApiResult?> = _testApiResult.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    // Parser live sandbox state
    private val _parserSender = MutableStateFlow("bKash")
    val parserSender: StateFlow<String> = _parserSender.asStateFlow()

    private val _parserInput = MutableStateFlow(SampleSmsData.samples[0].body)
    val parserInput: StateFlow<String> = _parserInput.asStateFlow()

    private val _parserResult = MutableStateFlow<ParsedTransactionResult?>(null)
    val parserResult: StateFlow<ParsedTransactionResult?> = _parserResult.asStateFlow()

    // Filtered transactions
    val filteredTransactions = combine(allTransactions, _filterStatus, _searchQuery) { list, status, query ->
        list.filter { tx ->
            val matchesStatus = when (status) {
                "ALL" -> true
                else -> tx.status.equals(status, ignoreCase = true)
            }
            val matchesQuery = query.isBlank() ||
                tx.transactionId.contains(query, ignoreCase = true) ||
                tx.provider.contains(query, ignoreCase = true) ||
                tx.customerReference.contains(query, ignoreCase = true) ||
                tx.amount.toString().contains(query)

            matchesStatus && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        testParseInput()
    }

    fun setTab(index: Int) {
        _selectedTab.value = index
    }

    fun setFilterStatus(status: String) {
        _filterStatus.value = status
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleLanguage() {
        val newIsBn = !_isBengali.value
        _isBengali.value = newIsBn
        settingsRepo.setLanguage(if (newIsBn) "bn" else "en")
    }

    fun acceptConsent() {
        _isConsentAccepted.value = true
        settingsRepo.setConsentAccepted(true)
        viewModelScope.launch {
            db.auditLogDao().insertLog(
                AuditLogEntity(
                    action = "CONSENT_ACCEPTED",
                    details = "Merchant acknowledged payment SMS reading consent terms."
                )
            )
        }
    }

    fun toggleMonitoring(context: Context) {
        val currentlyActive = _isMonitoring.value
        if (currentlyActive) {
            val stopIntent = Intent(context, SmsMonitoringService::class.java).apply {
                action = SmsMonitoringService.ACTION_STOP_MONITORING
            }
            context.startService(stopIntent)
            _isMonitoring.value = false
            _feedbackMessage.value = "SMS Payment monitoring stopped."
        } else {
            val startIntent = Intent(context, SmsMonitoringService::class.java).apply {
                action = SmsMonitoringService.ACTION_START_MONITORING
            }
            ContextCompat.startForegroundService(context, startIntent)
            _isMonitoring.value = true
            _feedbackMessage.value = "SMS Payment monitoring started with foreground notification."
        }
    }

    fun selectTransaction(tx: TransactionEntity?) {
        _selectedTransaction.value = tx
    }

    fun openSendPreview(tx: TransactionEntity) {
        _previewTransaction.value = tx
    }

    fun closeSendPreview() {
        _previewTransaction.value = null
    }

    fun confirmSendToGateway(tx: TransactionEntity) {
        viewModelScope.launch {
            _isSending.value = true
            val result = transactionRepo.sendToGateway(tx, _apiSettingsState.value)
            _isSending.value = false
            _previewTransaction.value = null

            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
            _lastSyncTime.value = timeFormat.format(Date())

            if (result.isSuccess) {
                _feedbackMessage.value = "Successfully submitted Txn ${tx.transactionId} to gateway (HTTP ${result.httpCode})!"
            } else {
                _feedbackMessage.value = "Submission failed: ${result.errorMessage ?: "HTTP " + result.httpCode}"
            }

            // Update detail view if open
            if (_selectedTransaction.value?.id == tx.id) {
                _selectedTransaction.value = transactionRepo.getTransactionById(tx.id)
            }
        }
    }

    fun markReviewed(tx: TransactionEntity) {
        viewModelScope.launch {
            val updated = tx.copy(needsReview = false, isManualReviewApproved = true)
            transactionRepo.updateTransaction(updated)
            if (_selectedTransaction.value?.id == tx.id) {
                _selectedTransaction.value = updated
            }
            _feedbackMessage.value = "Transaction marked as reviewed."
        }
    }

    fun ignoreTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            val updated = tx.copy(status = "IGNORED", needsReview = false)
            transactionRepo.updateTransaction(updated)
            if (_selectedTransaction.value?.id == tx.id) {
                _selectedTransaction.value = updated
            }
            _feedbackMessage.value = "Transaction marked as ignored."
        }
    }

    fun deleteTransaction(txId: Long) {
        viewModelScope.launch {
            transactionRepo.deleteTransaction(txId)
            if (_selectedTransaction.value?.id == txId) {
                _selectedTransaction.value = null
            }
            _feedbackMessage.value = "Record deleted locally."
        }
    }

    fun saveApiSettings(settings: ApiSettings, secret: String) {
        if (!settings.apiUrl.startsWith("https://", ignoreCase = true)) {
            _feedbackMessage.value = "Error: API URL must use HTTPS. Insecure HTTP is rejected."
            return
        }

        settingsRepo.saveApiSettings(settings, secret)
        _apiSettingsState.value = settings
        _apiSecretState.value = secret
        _feedbackMessage.value = "API configuration and Keystore secrets saved securely!"

        viewModelScope.launch {
            db.auditLogDao().insertLog(
                AuditLogEntity(
                    action = "API_SETTINGS_UPDATED",
                    details = "Updated reconciliation endpoint: ${settings.apiUrl}"
                )
            )
        }
    }

    fun testApiEndpoint() {
        viewModelScope.launch {
            _isSending.value = true
            val result = transactionRepo.testApiEndpoint(_apiSettingsState.value)
            _isSending.value = false
            _testApiResult.value = result
            if (result.isSuccess) {
                _feedbackMessage.value = "Test API Successful! Destination verified (HTTP ${result.httpCode})."
            } else {
                _feedbackMessage.value = "Test API Failed: ${result.errorMessage ?: "HTTP " + result.httpCode}"
            }
        }
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    // Parser Sandbox
    fun setParserSender(sender: String) {
        _parserSender.value = sender
        testParseInput()
    }

    fun setParserInput(text: String) {
        _parserInput.value = text
        testParseInput()
    }

    fun loadSampleIntoParser(sample: SampleSmsItem) {
        _parserSender.value = sample.sender
        _parserInput.value = sample.body
        testParseInput()
    }

    fun testParseInput() {
        val result = PaymentSmsProcessor.parseTransactionSms(_parserSender.value, _parserInput.value)
        _parserResult.value = result
    }

    // Simulator: simulate incoming SMS as if received by phone
    fun simulateIncomingSms(sample: SampleSmsItem) {
        viewModelScope.launch {
            val enabledSenders = db.approvedSenderDao().getEnabledSenders()
            if (!PaymentSmsProcessor.isApprovedSender(sample.sender, enabledSenders)) {
                _feedbackMessage.value = "Simulator: Sender '${sample.sender}' is not currently approved. Enable it first."
                return@launch
            }

            if (sample.isSensitiveTest || PaymentSmsProcessor.isSensitiveMessage(sample.body)) {
                db.auditLogDao().insertLog(
                    AuditLogEntity(
                        action = "SENSITIVE_SMS_IGNORED",
                        details = "Blocked OTP/PIN message from '${sample.sender}'."
                    )
                )
                _feedbackMessage.value = "Safety Filter Active: OTP/PIN message successfully blocked!"
                return@launch
            }

            val parsed = PaymentSmsProcessor.parseTransactionSms(sample.sender, sample.body)
            val hash = PaymentSmsProcessor.generateTransactionHash(parsed.provider, parsed.transactionId, parsed.amount)
            val isDuplicate = PaymentSmsProcessor.isDuplicateTransaction(parsed.transactionId, hash, allTransactions.value)

            val status = if (isDuplicate) "DUPLICATE" else "PENDING"
            val entity = TransactionEntity(
                transactionId = parsed.transactionId,
                provider = parsed.provider,
                senderId = parsed.senderId,
                amount = parsed.amount,
                currency = parsed.currency,
                customerReference = parsed.customerReference,
                balance = parsed.balance,
                originalSmsMasked = parsed.maskedOriginalSms,
                transactionHash = hash,
                confidenceScore = parsed.confidenceScore,
                timestamp = System.currentTimeMillis(),
                status = status,
                needsReview = parsed.needsReview || isDuplicate
            )

            transactionRepo.insertTransaction(entity)
            _feedbackMessage.value = "Simulated payment captured: ${parsed.provider} Tk ${parsed.amount} ($status)"
        }
    }

    fun addApprovedSender(pattern: String, provider: String, notes: String) {
        viewModelScope.launch {
            transactionRepo.addApprovedSender(pattern, provider, notes)
            _feedbackMessage.value = "Added approved sender: $pattern"
        }
    }

    fun toggleApprovedSender(sender: ApprovedSenderEntity) {
        viewModelScope.launch {
            val updated = sender.copy(isEnabled = !sender.isEnabled)
            transactionRepo.updateApprovedSender(updated)
        }
    }

    fun deleteApprovedSender(sender: ApprovedSenderEntity) {
        viewModelScope.launch {
            transactionRepo.deleteApprovedSender(sender)
            _feedbackMessage.value = "Deleted approved sender: ${sender.senderPattern}"
        }
    }

    fun exportTransactionsCsv(onResult: (File) -> Unit) {
        viewModelScope.launch {
            val file = transactionRepo.exportTransactionsToCsv(allTransactions.value)
            onResult(file)
            _feedbackMessage.value = "Report exported to: ${file.name}"
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            transactionRepo.clearHistory()
            _feedbackMessage.value = "Submission history cleared."
        }
    }

    fun wipeAllData() {
        viewModelScope.launch {
            transactionRepo.wipeAllData()
            settingsRepo.clearAllSettings()
            _isConsentAccepted.value = false
            _isMonitoring.value = false
            _selectedTransaction.value = null
            _feedbackMessage.value = "All local data, secrets, and logs have been wiped permanently."
        }
    }
}
