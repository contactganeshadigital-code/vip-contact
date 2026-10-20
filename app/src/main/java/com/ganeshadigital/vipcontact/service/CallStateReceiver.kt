package com.ganeshadigital.vipcontact.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.ganeshadigital.vipcontact.core.CallHandler
import com.ganeshadigital.vipcontact.core.RingController

/**
 * - RINGING: number milala (READ_CALL_LOG asel tar) -> VIP check
 * - OFFHOOK / IDLE: call uchalla kiva sampla -> ringtone band
 */
class CallStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        when (intent.getStringExtra(TelephonyManager.EXTRA_STATE)) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                @Suppress("DEPRECATION")
                val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                if (!number.isNullOrBlank()) CallHandler.onIncoming(context, number)
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK,
            TelephonyManager.EXTRA_STATE_IDLE -> {
                if (RingController.isRinging) RingController.stop(context)
            }
        }
    }
}
