package com.ganeshadigital.vipcontact.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.ganeshadigital.vipcontact.MainActivity
import com.ganeshadigital.vipcontact.R
import com.ganeshadigital.vipcontact.core.RingController

/**
 * Ringtone vajat astana app la background madhe zivant thevto
 * ani notification madhe "Ringtone band kara" button dakhvto.
 */
class RingService : Service() {

    companion object {
        private const val CHANNEL_ID = "vip_ring"
        private const val NOTIF_ID = 4101
        private const val EXTRA_TITLE = "title"

        fun start(ctx: Context, title: String) {
            val i = Intent(ctx, RingService::class.java).putExtra(EXTRA_TITLE, title)
            try {
                ContextCompat.startForegroundService(ctx, i)
            } catch (e: Exception) {
                // Background start nakarla tari ringtone RingController madhe vajat rahto
                Log.w("RingService", "Foreground start blocked", e)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "VIP कॉल"

        val stopPi = PendingIntent.getBroadcast(
            this, 1, Intent(this, StopReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val openPi = PendingIntent.getActivity(
            this, 2, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_vip)
            .setContentTitle("⭐ VIP कॉल: $title")
            .setContentText("फोन सायलेंट असूनही रिंगटोन वाजत आहे")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openPi)
            .addAction(0, "रिंगटोन बंद करा", stopPi)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIF_ID, notification)
            }
        } catch (e: Exception) {
            Log.w("RingService", "startForeground failed", e)
            stopSelf()
            return START_NOT_STICKY
        }

        if (!RingController.isRinging) stopSelf()
        return START_NOT_STICKY
    }

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val ch = NotificationChannel(CHANNEL_ID, "VIP रिंगटोन", NotificationManager.IMPORTANCE_LOW).apply {
            description = "VIP कॉल येत असताना दिसणारी सूचना"
            setSound(null, null)
            enableVibration(false)
        }
        nm.createNotificationChannel(ch)
    }
}
