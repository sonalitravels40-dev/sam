package com.example.data

data class TransactionEntity(
    val id: Long = 0,
    val transactionId: String,
    val provider: String,
    val senderId: String,
    val amount: Double,
    val currency: String = "BDT",
    val customerReference: String = "",
    val balance: Double? = null,
    val originalSmsMasked: String = "",
    val transactionHash: String = "",
    val confidenceScore: Int = 100,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "PENDING", // PENDING, SENT, FAILED, DUPLICATE, IGNORED
    val apiResponseCode: Int? = null,
    val apiResponseMessage: String? = null,
    val needsReview: Boolean = false,
    val isManualReviewApproved: Boolean = false
)

data class SubmissionHistoryEntity(
    val id: Long = 0,
    val transactionId: String,
    val provider: String,
    val amount: Double,
    val destinationDomain: String,
    val httpStatus: Int,
    val isSuccess: Boolean,
    val retryCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
)

data class ApprovedSenderEntity(
    val id: Long = 0,
    val senderPattern: String,
    val providerName: String,
    val isEnabled: Boolean = true,
    val notes: String = ""
)

data class AuditLogEntity(
    val id: Long = 0,
    val action: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)
