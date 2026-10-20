package com.ganeshadigital.vipcontact.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ganeshadigital.vipcontact.core.RingController

class StopReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        RingController.stop(context)
    }
}
