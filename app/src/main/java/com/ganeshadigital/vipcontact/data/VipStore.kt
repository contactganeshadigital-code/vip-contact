package com.ganeshadigital.vipcontact.data

import android.content.Context
import com.ganeshadigital.vipcontact.util.PhoneUtil
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class VipContact(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val number: String,
    val ringtoneUri: String? = null,
    val ringtoneName: String? = null,
    val volume: Int = 100
)

data class VipSettings(
    val enabled: Boolean,
    val onlyWhenQuiet: Boolean,
    val overrideDnd: Boolean,
    val vibrate: Boolean,
    val flash: Boolean,
    val repeatCaller: Boolean,
    val maxRingSeconds: Int,
    val defaultRingtoneUri: String?,
    val defaultRingtoneName: String?,
    val defaultVolume: Int
)

class VipStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("vip_contact_store", Context.MODE_PRIVATE)

    // ---------------- VIP contacts ----------------

    fun getAll(): List<VipContact> {
        val raw = prefs.getString(KEY_LIST, "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                VipContact(
                    id = o.optString("id").ifBlank { UUID.randomUUID().toString() },
                    name = o.optString("name"),
                    number = o.optString("number"),
                    ringtoneUri = o.optString("uri").ifBlank { null },
                    ringtoneName = o.optString("uriName").ifBlank { null },
                    volume = o.optInt("volume", 100)
                )
            }.sortedBy { it.name.lowercase() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveAll(list: List<VipContact>) {
        val arr = JSONArray()
        list.forEach { c ->
            arr.put(JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("number", c.number)
                put("volume", c.volume)
                c.ringtoneUri?.let { put("uri", it) }
                c.ringtoneName?.let { put("uriName", it) }
            })
        }
        prefs.edit().putString(KEY_LIST, arr.toString()).apply()
    }

    /** Navin kiva badal — same number cha juna entry kadhun takto */
    fun upsert(contact: VipContact) {
        val list = getAll()
            .filter { it.id != contact.id && it.number != contact.number }
            .toMutableList()
        list.add(contact)
        saveAll(list)
    }

    fun delete(id: String) = saveAll(getAll().filter { it.id != id })

    fun findByNumber(raw: String?): VipContact? {
        val n = PhoneUtil.normalize(raw)
        if (n.length < 5) return null
        return getAll().firstOrNull { it.number == n }
    }

    // ---------------- Settings ----------------

    var enabled: Boolean
        get() = prefs.getBoolean("enabled", true)
        set(v) = prefs.edit().putBoolean("enabled", v).apply()

    var onlyWhenQuiet: Boolean
        get() = prefs.getBoolean("only_quiet", true)
        set(v) = prefs.edit().putBoolean("only_quiet", v).apply()

    var overrideDnd: Boolean
        get() = prefs.getBoolean("override_dnd", true)
        set(v) = prefs.edit().putBoolean("override_dnd", v).apply()

    var vibrate: Boolean
        get() = prefs.getBoolean("vibrate", true)
        set(v) = prefs.edit().putBoolean("vibrate", v).apply()

    var flash: Boolean
        get() = prefs.getBoolean("flash", false)
        set(v) = prefs.edit().putBoolean("flash", v).apply()

    var repeatCaller: Boolean
        get() = prefs.getBoolean("repeat_caller", false)
        set(v) = prefs.edit().putBoolean("repeat_caller", v).apply()

    var maxRingSeconds: Int
        get() = prefs.getInt("max_ring_sec", 60)
        set(v) = prefs.edit().putInt("max_ring_sec", v).apply()

    var defaultRingtoneUri: String?
        get() = prefs.getString("def_uri", null)
        set(v) = prefs.edit().putString("def_uri", v).apply()

    var defaultRingtoneName: String?
        get() = prefs.getString("def_uri_name", null)
        set(v) = prefs.edit().putString("def_uri_name", v).apply()

    var defaultVolume: Int
        get() = prefs.getInt("def_volume", 100)
        set(v) = prefs.edit().putInt("def_volume", v).apply()

    fun settings() = VipSettings(
        enabled = enabled,
        onlyWhenQuiet = onlyWhenQuiet,
        overrideDnd = overrideDnd,
        vibrate = vibrate,
        flash = flash,
        repeatCaller = repeatCaller,
        maxRingSeconds = maxRingSeconds,
        defaultRingtoneUri = defaultRingtoneUri,
        defaultRingtoneName = defaultRingtoneName,
        defaultVolume = defaultVolume
    )

    // ---------------- Call tracking ----------------

    /**
     * Ekach call CallScreeningService ani PHONE_STATE broadcast doghanmadhun yeu shakto.
     * Kahi second madhe same number aala tar to duplicate samjto.
     */
    fun isDuplicateEvent(number: String): Boolean {
        val now = System.currentTimeMillis()
        val lastNum = prefs.getString("last_evt_num", null)
        val lastAt = prefs.getLong("last_evt_at", 0L)
        if (lastNum == number && now - lastAt < DUPLICATE_WINDOW_MS) return true
        prefs.edit().putString("last_evt_num", number).putLong("last_evt_at", now).apply()
        return false
    }

    /** Ha number magchya 3 minitat call kela hota ka */
    fun isRepeatCall(number: String): Boolean {
        val last = recentCalls().optLong(number, 0L)
        return last > 0L && System.currentTimeMillis() - last <= REPEAT_WINDOW_MS
    }

    fun recordCall(number: String) {
        val now = System.currentTimeMillis()
        val old = recentCalls()
        val fresh = JSONObject()
        old.keys().forEach { k ->
            val t = old.optLong(k, 0L)
            if (now - t < 10 * 60_000L) fresh.put(k, t)
        }
        fresh.put(number, now)
        prefs.edit().putString("recent_calls", fresh.toString()).apply()
    }

    private fun recentCalls(): JSONObject = try {
        JSONObject(prefs.getString("recent_calls", "{}") ?: "{}")
    } catch (e: Exception) {
        JSONObject()
    }

    companion object {
        private const val KEY_LIST = "vip_list"
        private const val DUPLICATE_WINDOW_MS = 6_000L
        private const val REPEAT_WINDOW_MS = 3 * 60_000L
    }
}
