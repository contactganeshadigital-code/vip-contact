package com.ganeshadigital.vipcontact.util

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.OpenableColumns

object Tones {

    fun pickerIntent(current: String?): Intent =
        Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "VIP रिंगटोन निवडा")
            current?.let { putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it)) }
        }

    fun title(ctx: Context, uri: Uri): String = try {
        RingtoneManager.getRingtone(ctx, uri)?.getTitle(ctx) ?: "रिंगटोन"
    } catch (e: Exception) {
        "रिंगटोन"
    }

    fun fileName(ctx: Context, uri: Uri): String = try {
        ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
            ?: "MP3 फाईल"
    } catch (e: Exception) {
        "MP3 फाईल"
    }
}
