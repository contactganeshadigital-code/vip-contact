package com.ganeshadigital.vipcontact.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

data class PermStatus(
    val phoneState: Boolean,
    val notifications: Boolean,
    val screeningAvailable: Boolean,
    val screeningRole: Boolean,
    val callLog: Boolean,
    val dnd: Boolean,
    val battery: Boolean
) {
    /** Number olakhnyacha kontatari ek marg tayar aahe ka */
    val numberDetect: Boolean get() = screeningRole || callLog
    val coreReady: Boolean get() = phoneState && notifications && numberDetect && battery
}

object Perms {

    fun has(ctx: Context, p: String): Boolean =
        ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED

    fun status(ctx: Context): PermStatus {
        val notif = if (Build.VERSION.SDK_INT >= 33) has(ctx, Manifest.permission.POST_NOTIFICATIONS) else true
        var available = false
        var held = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = ctx.getSystemService(RoleManager::class.java)
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                available = true
                held = rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
            }
        }
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val pm = ctx.getSystemService(PowerManager::class.java)
        val phone = has(ctx, Manifest.permission.READ_PHONE_STATE)
        return PermStatus(
            phoneState = phone,
            notifications = notif,
            screeningAvailable = available,
            screeningRole = held,
            callLog = phone && has(ctx, Manifest.permission.READ_CALL_LOG),
            dnd = nm?.isNotificationPolicyAccessGranted == true,
            battery = pm?.isIgnoringBatteryOptimizations(ctx.packageName) == true
        )
    }

    fun screeningRoleIntent(ctx: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val rm = ctx.getSystemService(RoleManager::class.java) ?: return null
        if (!rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) return null
        return rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
    }

    @SuppressLint("BatteryLife")
    fun batteryIntent(ctx: Context): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}"))

    fun batteryListIntent(): Intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

    fun dndIntent(): Intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)

    fun appDetailsIntent(ctx: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))

    /** Mi / Redmi / Poco, Oppo, Realme, Vivo, iQOO, Huawei cha Autostart screen */
    fun openAutostart(ctx: Context) {
        val comps = listOf(
            ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
            ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
            ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
            ComponentName("com.oplus.safecenter", "com.oplus.safecenter.permission.startup.StartupAppListActivity"),
            ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
            ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
            ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"),
            ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
            ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.autostart.AutoStartActivity")
        )
        for (c in comps) {
            try {
                ctx.startActivity(Intent().setComponent(c).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (_: Exception) {
            }
        }
        try {
            ctx.startActivity(appDetailsIntent(ctx).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
        }
    }
}
