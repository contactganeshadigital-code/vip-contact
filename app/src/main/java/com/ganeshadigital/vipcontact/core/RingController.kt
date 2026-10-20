package com.ganeshadigital.vipcontact.core

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import com.ganeshadigital.vipcontact.data.VipContact
import com.ganeshadigital.vipcontact.data.VipStore
import com.ganeshadigital.vipcontact.service.RingService

/**
 * VIP ringtone vajavnara engine.
 * Ringtone ALARM stream var vajto — tyamule phone Silent / Vibrate var asla tari awaj yeto.
 */
object RingController {

    private const val TAG = "VipRing"

    private val handler = Handler(Looper.getMainLooper())
    private var appCtx: Context? = null
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var savedAlarmVolume = -1
    private var savedFilter = -1
    private var torchId: String? = null
    private var torchOn = false

    /** UI sathi: ringtone vajat aahe ka */
    val ringing = mutableStateOf(false)

    val isRinging: Boolean get() = player != null

    private val stopRunnable = Runnable { appCtx?.let { stop(it) } }

    private val flashRunnable = object : Runnable {
        override fun run() {
            val id = torchId ?: return
            val ctx = appCtx ?: return
            try {
                torchOn = !torchOn
                ctx.getSystemService(CameraManager::class.java)?.setTorchMode(id, torchOn)
            } catch (e: Exception) {
                return
            }
            handler.postDelayed(this, 350L)
        }
    }

    /**
     * @param vip null asel tar default ringtone / volume vaprto
     * @param testMode true = 10 second test
     */
    fun start(context: Context, vip: VipContact?, testMode: Boolean = false): Boolean {
        val ctx = context.applicationContext
        if (isRinging) stop(ctx)
        appCtx = ctx
        val store = VipStore(ctx)

        // 1) DND (Total silence / Priority) asel tar alarms allow kara
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (store.overrideDnd && nm != null && nm.isNotificationPolicyAccessGranted) {
            val f = nm.currentInterruptionFilter
            if (f == NotificationManager.INTERRUPTION_FILTER_NONE ||
                f == NotificationManager.INTERRUPTION_FILTER_PRIORITY
            ) {
                try {
                    nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS)
                    savedFilter = f
                } catch (e: Exception) {
                    Log.w(TAG, "DND change failed", e)
                }
            }
        }

        // 2) Alarm stream cha awaj set kara (nantar parat juna karto)
        val am = ctx.getSystemService(AudioManager::class.java)
        val pct = (vip?.volume ?: store.defaultVolume).coerceIn(10, 100)
        if (am != null) {
            try {
                savedAlarmVolume = am.getStreamVolume(AudioManager.STREAM_ALARM)
                val max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                val target = (max * pct / 100).coerceAtLeast(1)
                am.setStreamVolume(AudioManager.STREAM_ALARM, target, 0)
            } catch (e: Exception) {
                savedAlarmVolume = -1
            }
        }

        // 3) Ringtone vajva — contact cha tone, nahitar default, nahitar phone cha
        val candidates = listOfNotNull(
            vip?.ringtoneUri,
            store.defaultRingtoneUri,
            RingtoneManager.getActualDefaultRingtoneUri(ctx, RingtoneManager.TYPE_RINGTONE)?.toString(),
            Settings.System.DEFAULT_RINGTONE_URI.toString(),
            Settings.System.DEFAULT_ALARM_ALERT_URI.toString()
        ).distinct()

        var mp: MediaPlayer? = null
        for (u in candidates) {
            mp = createPlayer(ctx, Uri.parse(u))
            if (mp != null) break
        }
        if (mp == null) {
            Log.e(TAG, "No playable ringtone")
            restoreAudio(ctx)
            return false
        }
        player = mp
        ringing.value = true

        if (store.vibrate) startVibrate(ctx)
        if (store.flash) startFlash(ctx)

        val seconds = if (testMode) 10 else store.maxRingSeconds.coerceIn(15, 180)
        handler.postDelayed(stopRunnable, seconds * 1000L)
        return true
    }

    fun stop(context: Context) {
        val ctx = context.applicationContext
        val wasRinging = isRinging
        handler.removeCallbacks(stopRunnable)
        handler.removeCallbacks(flashRunnable)

        player?.let {
            try { it.stop() } catch (_: Exception) {}
            try { it.release() } catch (_: Exception) {}
        }
        player = null

        vibrator?.cancel()
        vibrator = null

        torchId?.let { id ->
            try {
                ctx.getSystemService(CameraManager::class.java)?.setTorchMode(id, false)
            } catch (_: Exception) {}
        }
        torchId = null
        torchOn = false

        restoreAudio(ctx)
        ringing.value = false

        if (wasRinging) {
            try { ctx.stopService(Intent(ctx, RingService::class.java)) } catch (_: Exception) {}
        }
    }

    private fun restoreAudio(ctx: Context) {
        if (savedAlarmVolume >= 0) {
            try {
                ctx.getSystemService(AudioManager::class.java)
                    ?.setStreamVolume(AudioManager.STREAM_ALARM, savedAlarmVolume, 0)
            } catch (_: Exception) {}
            savedAlarmVolume = -1
        }
        if (savedFilter >= 0) {
            try {
                ctx.getSystemService(NotificationManager::class.java)?.setInterruptionFilter(savedFilter)
            } catch (_: Exception) {}
            savedFilter = -1
        }
    }

    private fun createPlayer(ctx: Context, uri: Uri): MediaPlayer? {
        val mp = MediaPlayer()
        return try {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            mp.setDataSource(ctx, uri)
            mp.isLooping = true
            mp.prepare()
            mp.start()
            mp
        } catch (e: Exception) {
            Log.w(TAG, "Cannot play $uri", e)
            try { mp.release() } catch (_: Exception) {}
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun startVibrate(ctx: Context) {
        val v: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ctx.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            ctx.getSystemService(Vibrator::class.java)
        }
        if (v == null || !v.hasVibrator()) return
        vibrator = v
        val effect = VibrationEffect.createWaveform(longArrayOf(0L, 900L, 700L), 0)
        val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
        try { v.vibrate(effect, attrs) } catch (_: Exception) {}
    }

    private fun startFlash(ctx: Context) {
        try {
            val cm = ctx.getSystemService(CameraManager::class.java) ?: return
            torchId = cm.cameraIdList.firstOrNull { id ->
                cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            if (torchId != null) handler.post(flashRunnable)
        } catch (e: Exception) {
            torchId = null
        }
    }
}
