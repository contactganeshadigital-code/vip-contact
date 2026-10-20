package com.ganeshadigital.vipcontact.service

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import com.ganeshadigital.vipcontact.core.CallHandler

/**
 * Android 10+ : app la "Caller ID & spam app" role dila ki system pratyek
 * yenarya call cha number ithe deto. Aapan call kadhich block karat nahi.
 */
class VipCallScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        // Call normal chalu dya
        try {
            respondToCall(callDetails, CallResponse.Builder().build())
        } catch (_: Exception) {}

        val incoming = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            callDetails.callDirection == Call.Details.DIRECTION_INCOMING
        } else {
            true
        }
        if (!incoming) return

        val number = callDetails.handle?.schemeSpecificPart
        CallHandler.onIncoming(applicationContext, number)
    }
}
