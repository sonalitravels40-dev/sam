package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.example.data.AppDatabase
import com.example.data.AuditLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsMonitoringService : Service() {

    companion object {
        const val ACTION_START_MONITORING = "com.example.service.START_MONITORING"
        const val ACTION_STOP_MONITORING = "com.example.service.STOP_MONITORING"
        private const val PREFS = "sms_reader_prefs"
        private const val KEY_MONITORING_ACTIVE = "monitoring_active"

        fun isMonitoringActive(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_MONITORING_ACTIVE, false)
        }

        fun setMonitoringActive(context: Context, active: Boolean) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_MONITORING_ACTIVE, active).apply()
        }
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_MONITORING) {
            stopMonitoring()
            return START_NOT_STICKY
        }

        startForegroundServiceNotification()
        setMonitoringActive(this, true)

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(applicationContext)
            db.auditLogDao().insertLog(
                AuditLogEntity(
                    action = "MONITORING_STARTED",
                    details = "Foreground SMS monitoring service activated with merchant consent."
                )
            )
        }

        return START_STICKY
    }

    private fun startForegroundServiceNotification() {
        val notification = NotificationHelper.buildMonitoringNotification(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NotificationHelper.MONITORING_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NotificationHelper.MONITORING_NOTIFICATION_ID, notification)
        }
    }

    private fun stopMonitoring() {
        setMonitoringActive(this, false)
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(applicationContext)
            db.auditLogDao().insertLog(
                AuditLogEntity(
                    action = "MONITORING_STOPPED",
                    details = "Foreground SMS monitoring service stopped by merchant request."
                )
            )
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
