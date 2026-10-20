package com.ganeshadigital.vipcontact.core

import android.content.Context
import android.util.Log
import com.ganeshadigital.vipcontact.data.VipStore
import com.ganeshadigital.vipcontact.service.RingService
import com.ganeshadigital.vipcontact.util.PhoneUtil

/** Yenarya call cha nirnay: VIP aahe ka, vajvayche ka */
object CallHandler {

    fun onIncoming(context: Context, rawNumber: String?) {
        val ctx = context.applicationContext
        val store = VipStore(ctx)
        if (!store.enabled) return

        val number = PhoneUtil.normalize(rawNumber)
        if (number.length < 5) return
        if (RingController.isRinging) return
        if (store.isDuplicateEvent(number)) return

        val vip = store.findByNumber(number)
        val repeat = vip == null && store.repeatCaller && store.isRepeatCall(number)
        store.recordCall(number)

        if (vip == null && !repeat) return
        if (store.onlyWhenQuiet && !PhoneUtil.isPhoneQuiet(ctx)) return

        Log.i("VipCall", "VIP call from ${vip?.name ?: number} (repeat=$repeat)")
        if (RingController.start(ctx, vip)) {
            val title = when {
                vip != null -> vip.name.ifBlank { number }
                else -> "$number (पुन्हा कॉल)"
            }
            RingService.start(ctx, title)
        }
    }
}
