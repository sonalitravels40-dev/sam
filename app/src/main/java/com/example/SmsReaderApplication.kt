package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.service.NotificationHelper

class SmsReaderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
        // Warm up database
        AppDatabase.getInstance(this)
    }
}
