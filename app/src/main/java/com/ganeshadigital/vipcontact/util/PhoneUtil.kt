package com.ganeshadigital.vipcontact.util

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager

object PhoneUtil {

    /** +91 98765 43210 / 09876543210 / 9876543210 -> 9876543210 */
    fun normalize(raw: String?): String {
        val digits = raw.orEmpty().filter { it.isDigit() }
        return if (digits.length > 10) digits.takeLast(10) else digits
    }

    /** Phone Silent / Vibrate / DND / ring volume 0 aahe ka */
    fun isPhoneQuiet(ctx: Context): Boolean {
        val am = ctx.getSystemService(AudioManager::class.java) ?: return true
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val dndOn = nm != null &&
            nm.currentInterruptionFilter > NotificationManager.INTERRUPTION_FILTER_ALL
        return am.ringerMode != AudioManager.RINGER_MODE_NORMAL ||
            am.getStreamVolume(AudioManager.STREAM_RING) == 0 ||
            dndOn
    }

    fun ringerLabel(ctx: Context): String {
        val am = ctx.getSystemService(AudioManager::class.java) ?: return "-"
        return when (am.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> "Silent 🔇"
            AudioManager.RINGER_MODE_VIBRATE -> "Vibrate 📳"
            else -> "Normal 🔔"
        }
    }
}
