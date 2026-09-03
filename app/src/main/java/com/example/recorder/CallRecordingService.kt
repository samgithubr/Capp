package com.example.recorder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class CallRecordingService : Service() {

    companion object {
        const val CHANNEL_ID = "coffee_call_recording_channel"
        const val NOTIFICATION_ID = 2001
        const val ACTION_START = "com.example.recorder.ACTION_START"
        const val ACTION_STOP = "com.example.recorder.ACTION_STOP"
        const val EXTRA_SHOP_NAME = "extra_shop_name"

        var isServiceRunning = false
            private set
    }

    private var recorderManager: AudioRecorderManager? = null

    override fun onCreate() {
        super.onCreate()
        recorderManager = AudioRecorderManager(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val shopName = intent?.getStringExtra(EXTRA_SHOP_NAME) ?: "Coffee Shop"

        when (action) {
            ACTION_START -> {
                isServiceRunning = true
                val notification = buildNotification(shopName)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
                recorderManager?.startRecording(shopName)
            }
            ACTION_STOP -> {
                recorderManager?.stopRecording()
                isServiceRunning = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Coffee Call Recorder",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows status while recording coffee shop phone order call"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(shopName: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpenApp = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, CallRecordingService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Recording Coffee Order Call")
            .setContentText("Shop: $shopName • Audio saved locally")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setContentIntent(pendingOpenApp)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Recording", pendingStop)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        recorderManager?.stopRecording()
        isServiceRunning = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
