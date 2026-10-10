package com.example.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppDatabase private constructor(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    "sms_reader_x_database.db",
    null,
    1
) {

    private val dbScope = CoroutineScope(Dispatchers.IO)

    // In-memory reactive state flows synced with SQLite
    private val _transactionsFlow = MutableStateFlow<List<TransactionEntity>>(emptyList())
    private val _historyFlow = MutableStateFlow<List<SubmissionHistoryEntity>>(emptyList())
    private val _sendersFlow = MutableStateFlow<List<ApprovedSenderEntity>>(emptyList())
    private val _auditLogsFlow = MutableStateFlow<List<AuditLogEntity>>(emptyList())

    init {
        // Load initial records into flows
        dbScope.launch {
            refreshAll()
            // Check default senders
            if (approvedSenderDao().getSenderCount() == 0) {
                approvedSenderDao().insertSenders(
                    listOf(
                        ApprovedSenderEntity(
                            senderPattern = "bKash",
                            providerName = "bKash",
                            isEnabled = true,
                            notes = "bKash Merchant/Customer payment notifications"
                        ),
                        ApprovedSenderEntity(
                            senderPattern = "NAGAD",
                            providerName = "Nagad",
                            isEnabled = true,
                            notes = "Nagad Payment & Money Received alerts"
                        ),
                        ApprovedSenderEntity(
                            senderPattern = "16216",
                            providerName = "Rocket",
                            isEnabled = true,
                            notes = "DBBL Rocket Payment confirmation"
                        ),
                        ApprovedSenderEntity(
                            senderPattern = "Upay",
                            providerName = "Upay",
                            isEnabled = true,
                            notes = "UCB Upay Merchant payment notifications"
                        )
                    )
                )
                auditLogDao().insertLog(
                    AuditLogEntity(
                        action = "APP_INITIALIZED",
                        details = "Database initialized with standard default payment providers."
                    )
                )
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                transactionId TEXT NOT NULL,
                provider TEXT NOT NULL,
                senderId TEXT NOT NULL,
                amount REAL NOT NULL,
                currency TEXT NOT NULL,
                customerReference TEXT,
                balance REAL,
                originalSmsMasked TEXT,
                transactionHash TEXT,
                confidenceScore INTEGER,
                timestamp INTEGER,
                status TEXT,
                apiResponseCode INTEGER,
                apiResponseMessage TEXT,
                needsReview INTEGER,
                isManualReviewApproved INTEGER
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS submission_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                transactionId TEXT NOT NULL,
                provider TEXT NOT NULL,
                amount REAL NOT NULL,
                destinationDomain TEXT NOT NULL,
                httpStatus INTEGER NOT NULL,
                isSuccess INTEGER NOT NULL,
                retryCount INTEGER,
                timestamp INTEGER,
                errorMessage TEXT
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS approved_senders (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                senderPattern TEXT NOT NULL,
                providerName TEXT NOT NULL,
                isEnabled INTEGER NOT NULL,
                notes TEXT
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS audit_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                action TEXT NOT NULL,
                details TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Simple upgrade
    }

    private suspend fun refreshAll() = withContext(Dispatchers.IO) {
        refreshTransactions()
        refreshHistory()
        refreshSenders()
        refreshLogs()
    }

    private fun refreshTransactions() {
        val list = mutableListOf<TransactionEntity>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM transactions ORDER BY timestamp DESC", null)
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(cursorToTransaction(c))
            }
        }
        _transactionsFlow.value = list
    }

    private fun refreshHistory() {
        val list = mutableListOf<SubmissionHistoryEntity>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM submission_history ORDER BY timestamp DESC", null)
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    SubmissionHistoryEntity(
                        id = c.getLong(c.getColumnIndexOrThrow("id")),
                        transactionId = c.getString(c.getColumnIndexOrThrow("transactionId")),
                        provider = c.getString(c.getColumnIndexOrThrow("provider")),
                        amount = c.getDouble(c.getColumnIndexOrThrow("amount")),
                        destinationDomain = c.getString(c.getColumnIndexOrThrow("destinationDomain")),
                        httpStatus = c.getInt(c.getColumnIndexOrThrow("httpStatus")),
                        isSuccess = c.getInt(c.getColumnIndexOrThrow("isSuccess")) == 1,
                        retryCount = c.getInt(c.getColumnIndexOrThrow("retryCount")),
                        timestamp = c.getLong(c.getColumnIndexOrThrow("timestamp")),
                        errorMessage = c.getString(c.getColumnIndexOrThrow("errorMessage"))
                    )
                )
            }
        }
        _historyFlow.value = list
    }

    private fun refreshSenders() {
        val list = mutableListOf<ApprovedSenderEntity>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM approved_senders ORDER BY providerName ASC", null)
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    ApprovedSenderEntity(
                        id = c.getLong(c.getColumnIndexOrThrow("id")),
                        senderPattern = c.getString(c.getColumnIndexOrThrow("senderPattern")),
                        providerName = c.getString(c.getColumnIndexOrThrow("providerName")),
                        isEnabled = c.getInt(c.getColumnIndexOrThrow("isEnabled")) == 1,
                        notes = c.getString(c.getColumnIndexOrThrow("notes")) ?: ""
                    )
                )
            }
        }
        _sendersFlow.value = list
    }

    private fun refreshLogs() {
        val list = mutableListOf<AuditLogEntity>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 100", null)
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    AuditLogEntity(
                        id = c.getLong(c.getColumnIndexOrThrow("id")),
                        action = c.getString(c.getColumnIndexOrThrow("action")),
                        details = c.getString(c.getColumnIndexOrThrow("details")),
                        timestamp = c.getLong(c.getColumnIndexOrThrow("timestamp"))
                    )
                )
            }
        }
        _auditLogsFlow.value = list
    }

    private fun cursorToTransaction(c: Cursor): TransactionEntity {
        val balanceCol = c.getColumnIndexOrThrow("balance")
        val balanceVal = if (c.isNull(balanceCol)) null else c.getDouble(balanceCol)
        val apiRespCodeCol = c.getColumnIndexOrThrow("apiResponseCode")
        val apiRespCode = if (c.isNull(apiRespCodeCol)) null else c.getInt(apiRespCodeCol)

        return TransactionEntity(
            id = c.getLong(c.getColumnIndexOrThrow("id")),
            transactionId = c.getString(c.getColumnIndexOrThrow("transactionId")),
            provider = c.getString(c.getColumnIndexOrThrow("provider")),
            senderId = c.getString(c.getColumnIndexOrThrow("senderId")),
            amount = c.getDouble(c.getColumnIndexOrThrow("amount")),
            currency = c.getString(c.getColumnIndexOrThrow("currency")) ?: "BDT",
            customerReference = c.getString(c.getColumnIndexOrThrow("customerReference")) ?: "",
            balance = balanceVal,
            originalSmsMasked = c.getString(c.getColumnIndexOrThrow("originalSmsMasked")) ?: "",
            transactionHash = c.getString(c.getColumnIndexOrThrow("transactionHash")) ?: "",
            confidenceScore = c.getInt(c.getColumnIndexOrThrow("confidenceScore")),
            timestamp = c.getLong(c.getColumnIndexOrThrow("timestamp")),
            status = c.getString(c.getColumnIndexOrThrow("status")) ?: "PENDING",
            apiResponseCode = apiRespCode,
            apiResponseMessage = c.getString(c.getColumnIndexOrThrow("apiResponseMessage")),
            needsReview = c.getInt(c.getColumnIndexOrThrow("needsReview")) == 1,
            isManualReviewApproved = c.getInt(c.getColumnIndexOrThrow("isManualReviewApproved")) == 1
        )
    }

    private val _transactionDao = object : TransactionDao {
        override fun getAllTransactions(): Flow<List<TransactionEntity>> = _transactionsFlow.asStateFlow()

        override fun getTransactionsByStatus(status: String): Flow<List<TransactionEntity>> =
            _transactionsFlow.map { list -> list.filter { it.status.equals(status, ignoreCase = true) } }

        override suspend fun getTransactionById(id: Long): TransactionEntity? = withContext(Dispatchers.IO) {
            _transactionsFlow.value.find { it.id == id }
        }

        override suspend fun findByTransactionId(txnId: String): TransactionEntity? = withContext(Dispatchers.IO) {
            _transactionsFlow.value.find { it.transactionId.equals(txnId, ignoreCase = true) }
        }

        override suspend fun findByHash(hash: String): TransactionEntity? = withContext(Dispatchers.IO) {
            _transactionsFlow.value.find { it.transactionHash == hash }
        }

        override suspend fun insertTransaction(transaction: TransactionEntity): Long = withContext(Dispatchers.IO) {
            val db = writableDatabase
            val values = ContentValues().apply {
                if (transaction.id > 0) put("id", transaction.id)
                put("transactionId", transaction.transactionId)
                put("provider", transaction.provider)
                put("senderId", transaction.senderId)
                put("amount", transaction.amount)
                put("currency", transaction.currency)
                put("customerReference", transaction.customerReference)
                if (transaction.balance != null) put("balance", transaction.balance) else putNull("balance")
                put("originalSmsMasked", transaction.originalSmsMasked)
                put("transactionHash", transaction.transactionHash)
                put("confidenceScore", transaction.confidenceScore)
                put("timestamp", transaction.timestamp)
                put("status", transaction.status)
                if (transaction.apiResponseCode != null) put("apiResponseCode", transaction.apiResponseCode) else putNull("apiResponseCode")
                put("apiResponseMessage", transaction.apiResponseMessage)
                put("needsReview", if (transaction.needsReview) 1 else 0)
                put("isManualReviewApproved", if (transaction.isManualReviewApproved) 1 else 0)
            }
            val id = db.insertWithOnConflict("transactions", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            refreshTransactions()
            id
        }

        override suspend fun updateTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
            insertTransaction(transaction)
            Unit
        }

        override suspend fun deleteTransaction(transaction: TransactionEntity) = deleteById(transaction.id)

        override suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
            writableDatabase.delete("transactions", "id = ?", arrayOf(id.toString()))
            refreshTransactions()
            Unit
        }

        override suspend fun deleteAll() = withContext(Dispatchers.IO) {
            writableDatabase.delete("transactions", null, null)
            refreshTransactions()
            Unit
        }

        override fun getCount(): Flow<Int> = _transactionsFlow.map { it.size }
        override fun getPendingCount(): Flow<Int> = _transactionsFlow.map { it.count { tx -> tx.status == "PENDING" } }
        override fun getSentCount(): Flow<Int> = _transactionsFlow.map { it.count { tx -> tx.status == "SENT" } }
        override fun getFailedCount(): Flow<Int> = _transactionsFlow.map { it.count { tx -> tx.status == "FAILED" } }
    }

    private val _submissionHistoryDao = object : SubmissionHistoryDao {
        override fun getAllHistory(): Flow<List<SubmissionHistoryEntity>> = _historyFlow.asStateFlow()

        override suspend fun insertHistory(history: SubmissionHistoryEntity): Long = withContext(Dispatchers.IO) {
            val db = writableDatabase
            val values = ContentValues().apply {
                if (history.id > 0) put("id", history.id)
                put("transactionId", history.transactionId)
                put("provider", history.provider)
                put("amount", history.amount)
                put("destinationDomain", history.destinationDomain)
                put("httpStatus", history.httpStatus)
                put("isSuccess", if (history.isSuccess) 1 else 0)
                put("retryCount", history.retryCount)
                put("timestamp", history.timestamp)
                put("errorMessage", history.errorMessage)
            }
            val id = db.insertWithOnConflict("submission_history", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            refreshHistory()
            id
        }

        override suspend fun clearHistory() = withContext(Dispatchers.IO) {
            writableDatabase.delete("submission_history", null, null)
            refreshHistory()
            Unit
        }
    }

    private val _approvedSenderDao = object : ApprovedSenderDao {
        override fun getAllSenders(): Flow<List<ApprovedSenderEntity>> = _sendersFlow.asStateFlow()

        override suspend fun getEnabledSenders(): List<ApprovedSenderEntity> = withContext(Dispatchers.IO) {
            _sendersFlow.value.filter { it.isEnabled }
        }

        override suspend fun insertSender(sender: ApprovedSenderEntity): Long = withContext(Dispatchers.IO) {
            val db = writableDatabase
            val values = ContentValues().apply {
                if (sender.id > 0) put("id", sender.id)
                put("senderPattern", sender.senderPattern)
                put("providerName", sender.providerName)
                put("isEnabled", if (sender.isEnabled) 1 else 0)
                put("notes", sender.notes)
            }
            val id = db.insertWithOnConflict("approved_senders", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            refreshSenders()
            id
        }

        override suspend fun insertSenders(senders: List<ApprovedSenderEntity>) = withContext(Dispatchers.IO) {
            for (s in senders) {
                insertSender(s)
            }
        }

        override suspend fun updateSender(sender: ApprovedSenderEntity) {
            insertSender(sender)
        }

        override suspend fun deleteSender(sender: ApprovedSenderEntity) = withContext(Dispatchers.IO) {
            writableDatabase.delete("approved_senders", "id = ?", arrayOf(sender.id.toString()))
            refreshSenders()
            Unit
        }

        override suspend fun getSenderCount(): Int = withContext(Dispatchers.IO) {
            _sendersFlow.value.size
        }
    }

    private val _auditLogDao = object : AuditLogDao {
        override fun getRecentLogs(): Flow<List<AuditLogEntity>> = _auditLogsFlow.asStateFlow()

        override suspend fun insertLog(log: AuditLogEntity): Long = withContext(Dispatchers.IO) {
            val db = writableDatabase
            val values = ContentValues().apply {
                if (log.id > 0) put("id", log.id)
                put("action", log.action)
                put("details", log.details)
                put("timestamp", log.timestamp)
            }
            val id = db.insertWithOnConflict("audit_logs", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            refreshLogs()
            id
        }

        override suspend fun clearLogs() = withContext(Dispatchers.IO) {
            writableDatabase.delete("audit_logs", null, null)
            refreshLogs()
            Unit
        }
    }

    fun transactionDao(): TransactionDao = _transactionDao
    fun submissionHistoryDao(): SubmissionHistoryDao = _submissionHistoryDao
    fun approvedSenderDao(): ApprovedSenderDao = _approvedSenderDao
    fun auditLogDao(): AuditLogDao = _auditLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = AppDatabase(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
