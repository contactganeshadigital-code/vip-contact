@file:OptIn(ExperimentalMaterial3Api::class)

package com.ganeshadigital.vipcontact.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.IntentCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ganeshadigital.vipcontact.core.RingController
import com.ganeshadigital.vipcontact.data.VipContact
import com.ganeshadigital.vipcontact.data.VipSettings
import com.ganeshadigital.vipcontact.data.VipStore
import com.ganeshadigital.vipcontact.util.PermStatus
import com.ganeshadigital.vipcontact.util.Perms
import com.ganeshadigital.vipcontact.util.PhoneUtil
import com.ganeshadigital.vipcontact.util.Tones
import kotlin.math.roundToInt

private const val TARGET_DRAFT = 0
private const val TARGET_DEFAULT = 1

/** Setup screen che sagle button */
class SetupActions(
    val phoneState: () -> Unit,
    val notifications: () -> Unit,
    val screeningRole: () -> Unit,
    val callLog: () -> Unit,
    val dnd: () -> Unit,
    val battery: () -> Unit,
    val autostart: () -> Unit,
    val appSettings: () -> Unit
)

@Composable
fun VipApp() {
    val ctx = LocalContext.current
    val store = remember { VipStore(ctx) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var refresh by remember { mutableIntStateOf(0) }

    // App madhe parat aala ki (settings madhun) status refresh
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) refresh++ }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    val contacts = remember(refresh) { store.getAll() }
    val settings = remember(refresh) { store.settings() }
    val perms = remember(refresh) { Perms.status(ctx) }
    val ringerLabel = remember(refresh) { PhoneUtil.ringerLabel(ctx) }
    val ringing by RingController.ringing

    var draft by remember { mutableStateOf<VipContact?>(null) }
    var draftIsNew by remember { mutableStateOf(false) }
    var toneTarget by remember { mutableIntStateOf(TARGET_DRAFT) }

    fun toast(msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()

    fun launchSafe(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            toast("हे सेटिंग या फोनवर उघडत नाही")
        }
    }

    fun applyTone(uri: String, title: String) {
        if (toneTarget == TARGET_DEFAULT) {
            store.defaultRingtoneUri = uri
            store.defaultRingtoneName = title
            refresh++
        } else {
            draft = draft?.copy(ringtoneUri = uri, ringtoneName = title)
        }
    }

    // ---------- Launchers ----------
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refresh++ }

    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { refresh++ }

    val tonePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val data = res.data ?: return@rememberLauncherForActivityResult
        val uri = IntentCompat.getParcelableExtra(
            data, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java
        ) ?: return@rememberLauncherForActivityResult
        applyTone(uri.toString(), Tones.title(ctx, uri))
    }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Exception) {
        }
        applyTone(uri.toString(), Tones.fileName(ctx, uri))
    }

    val contactPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        val uri = res.data?.data ?: return@rememberLauncherForActivityResult
        var name = ""
        var number = ""
        try {
            ctx.contentResolver.query(
                uri,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null, null, null
            )?.use { c ->
                if (c.moveToFirst()) {
                    name = c.getString(0) ?: ""
                    number = c.getString(1) ?: ""
                }
            }
        } catch (_: Exception) {
        }
        if (number.isBlank()) {
            toast("नंबर मिळाला नाही, हाताने टाका")
            return@rememberLauncherForActivityResult
        }
        val existing = store.findByNumber(number)
        if (existing != null) {
            draft = existing
            draftIsNew = false
        } else {
            draft = VipContact(name = name, number = PhoneUtil.normalize(number), volume = settings.defaultVolume)
            draftIsNew = true
        }
    }

    fun addFromContacts() = launchSafe {
        contactPicker.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI))
    }

    fun addManual() {
        draft = VipContact(name = "", number = "", volume = settings.defaultVolume)
        draftIsNew = true
    }

    fun pickSystemTone(target: Int, current: String?) {
        toneTarget = target
        launchSafe { tonePicker.launch(Tones.pickerIntent(current)) }
    }

    fun pickFileTone(target: Int) {
        toneTarget = target
        launchSafe { filePicker.launch(arrayOf("audio/*")) }
    }

    val actions = SetupActions(
        phoneState = { permLauncher.launch(arrayOf(Manifest.permission.READ_PHONE_STATE)) },
        notifications = {
            if (Build.VERSION.SDK_INT >= 33) permLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
        },
        screeningRole = {
            val i = Perms.screeningRoleIntent(ctx)
            if (i == null) toast("या फोनवर हा पर्याय नाही, कॉल लॉग परवानगी द्या")
            else launchSafe { settingsLauncher.launch(i) }
        },
        callLog = {
            permLauncher.launch(arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_CALL_LOG))
        },
        dnd = { launchSafe { settingsLauncher.launch(Perms.dndIntent()) } },
        battery = {
            try {
                settingsLauncher.launch(Perms.batteryIntent(ctx))
            } catch (e: Exception) {
                launchSafe { settingsLauncher.launch(Perms.batteryListIntent()) }
            }
        },
        autostart = { Perms.openAutostart(ctx) },
        appSettings = { launchSafe { settingsLauncher.launch(Perms.appDetailsIntent(ctx)) } }
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Star, null, tint = MaterialTheme.colorScheme.primaryContainer)
                        Spacer(Modifier.width(6.dp))
                        Text("VIP CONTACT", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    titleContentColor = MaterialTheme.colorScheme.onSecondary
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0, onClick = { tab = 0 },
                    icon = { Icon(Icons.Filled.Star, null) }, label = { Text("VIP यादी") }
                )
                NavigationBarItem(
                    selected = tab == 1, onClick = { tab = 1 },
                    icon = {
                        Icon(
                            if (perms.coreReady) Icons.Filled.CheckCircle else Icons.Filled.Warning, null
                        )
                    },
                    label = { Text("सेटअप") }
                )
                NavigationBarItem(
                    selected = tab == 2, onClick = { tab = 2 },
                    icon = { Icon(Icons.Filled.Settings, null) }, label = { Text("सेटिंग") }
                )
            }
        },
        floatingActionButton = {
            if (tab == 0) {
                ExtendedFloatingActionButton(
                    onClick = { addFromContacts() },
                    icon = { Icon(Icons.Filled.Add, null) },
                    text = { Text("VIP जोडा") }
                )
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> HomeScreen(
                    contacts = contacts,
                    settings = settings,
                    perms = perms,
                    ringerLabel = ringerLabel,
                    onToggleEnabled = { store.enabled = it; refresh++ },
                    onOpenSetup = { tab = 1 },
                    onEdit = { draft = it; draftIsNew = false },
                    onManualAdd = { addManual() }
                )
                1 -> SetupScreen(perms = perms, actions = actions)
                else -> SettingsScreen(
                    settings = settings,
                    perms = perms,
                    ringing = ringing,
                    store = store,
                    onChanged = { refresh++ },
                    onPickSystemTone = { pickSystemTone(TARGET_DEFAULT, settings.defaultRingtoneUri) },
                    onPickFile = { pickFileTone(TARGET_DEFAULT) },
                    onClearTone = {
                        store.defaultRingtoneUri = null
                        store.defaultRingtoneName = null
                        refresh++
                    },
                    onTest = { RingController.start(ctx, null, testMode = true) },
                    onStopTest = { RingController.stop(ctx) },
                    onOpenDnd = actions.dnd
                )
            }
        }
    }

    draft?.let { d ->
        EditContactDialog(
            draft = d,
            isNew = draftIsNew,
            defaultToneName = settings.defaultRingtoneName ?: "फोनचा रिंगटोन",
            ringing = ringing,
            onChange = { draft = it },
            onPickSystemTone = { pickSystemTone(TARGET_DRAFT, d.ringtoneUri) },
            onPickFile = { pickFileTone(TARGET_DRAFT) },
            onTest = { RingController.start(ctx, d, testMode = true) },
            onStopTest = { RingController.stop(ctx) },
            onSave = {
                val n = PhoneUtil.normalize(d.number)
                store.upsert(d.copy(number = n, name = d.name.trim().ifBlank { n }))
                RingController.stop(ctx)
                draft = null
                refresh++
                toast("VIP सेव्ह झाला ⭐")
            },
            onDelete = {
                store.delete(d.id)
                RingController.stop(ctx)
                draft = null
                refresh++
            },
            onDismiss = {
                RingController.stop(ctx)
                draft = null
            }
        )
    }
}

// =====================================================================
// VIP यादी
// =====================================================================

@Composable
private fun HomeScreen(
    contacts: List<VipContact>,
    settings: VipSettings,
    perms: PermStatus,
    ringerLabel: String,
    onToggleEnabled: (Boolean) -> Unit,
    onOpenSetup: () -> Unit,
    onEdit: (VipContact) -> Unit,
    onManualAdd: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (settings.enabled) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (settings.enabled) "VIP संरक्षण चालू" else "VIP संरक्षण बंद",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "फोन सध्या: $ringerLabel",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            if (settings.onlyWhenQuiet) "सायलेंट / व्हायब्रेट असताना VIP कॉलला आवाज येईल"
                            else "प्रत्येक VIP कॉलला स्वतःचा रिंगटोन वाजेल",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(checked = settings.enabled, onCheckedChange = onToggleEnabled)
                }
            }
        }

        if (!perms.coreReady) {
            item {
                Card(
                    onClick = onOpenSetup,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "सेटअप अपूर्ण आहे",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                "परवानग्या दिल्याशिवाय VIP कॉलला आवाज येणार नाही. इथे टॅप करा.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "VIP यादी (${contacts.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onManualAdd) { Text("+ नंबर टाका") }
            }
        }

        if (contacts.isEmpty()) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.Star, null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.size(8.dp))
                    Text("अजून एकही VIP नाही", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "खालील \"VIP जोडा\" दाबून कॉन्टॅक्ट निवडा",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        items(contacts, key = { it.id }) { c ->
            ContactCard(c, settings.defaultRingtoneName, onClick = { onEdit(c) })
        }
    }
}

@Composable
private fun ContactCard(c: VipContact, defaultToneName: String?, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    c.name.trim().take(1).uppercase().ifBlank { "★" },
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    c.name.ifBlank { c.number },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(c.number, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "🎵 ${c.ringtoneName ?: (defaultToneName ?: "डिफॉल्ट टोन")} • 🔊 ${c.volume}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.Filled.Star, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

// =====================================================================
// सेटअप
// =====================================================================

@Composable
private fun SetupScreen(perms: PermStatus, actions: SetupActions) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text("फोन सेटअप", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "खालील पायऱ्या एकदाच पूर्ण करा. हिरवा ✓ आला की ती पायरी पूर्ण.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        item {
            SetupItem(
                "फोन स्टेट परवानगी",
                "कॉल उचलला किंवा कट झाला की VIP रिंगटोन लगेच बंद होण्यासाठी.",
                perms.phoneState, "परवानगी द्या", actions.phoneState
            )
        }
        if (Build.VERSION.SDK_INT >= 33) {
            item {
                SetupItem(
                    "नोटिफिकेशन परवानगी",
                    "VIP कॉल वाजत असताना \"रिंगटोन बंद करा\" बटण दिसण्यासाठी.",
                    perms.notifications, "परवानगी द्या", actions.notifications
                )
            }
        }
        if (perms.screeningAvailable) {
            item {
                SetupItem(
                    "VIP नंबर ओळख (Caller ID app)",
                    "पुढच्या स्क्रीनवर VIP CONTACT ला \"Caller ID & spam app\" म्हणून निवडा. यामुळे येणारा नंबर ओळखता येतो. (Truecaller ठेवायचे असेल तर ही पायरी सोडा आणि खालची कॉल लॉग परवानगी द्या.)",
                    perms.screeningRole, "निवडा", actions.screeningRole
                )
            }
        }
        item {
            SetupItem(
                "कॉल लॉग परवानगी" + if (perms.screeningAvailable) " (पर्यायी)" else "",
                if (perms.screeningAvailable)
                    "वरची Caller ID पायरी केली असेल तर ही गरजेची नाही. Truecaller वापरत असाल तर ही द्या."
                else "VIP नंबर ओळखण्यासाठी आवश्यक (Android 9 किंवा जुना फोन).",
                if (perms.screeningRole && !perms.callLog) null else perms.callLog,
                "परवानगी द्या", actions.callLog
            )
        }
        item {
            SetupItem(
                "Do Not Disturb परवानगी",
                "DND चालू असतानाही VIP कॉलला आवाज येण्यासाठी. यादीत VIP CONTACT चालू करा.",
                perms.dnd, "सेटिंग उघडा", actions.dnd
            )
        }
        item {
            SetupItem(
                "बॅटरी ऑप्टिमायझेशन बंद",
                "फोन ॲप बंद करू नये म्हणून. \"Allow\" दाबा.",
                perms.battery, "बंद करा", actions.battery
            )
        }
        item {
            SetupItem(
                "Autostart (Mi, Redmi, Vivo, Oppo, Realme)",
                "या फोनमध्ये Autostart मध्ये VIP CONTACT चालू करा. Samsung मध्ये: Battery → Unrestricted.",
                null, "सेटिंग उघडा", actions.autostart
            )
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Text("चाचणी कशी करायची?", fontWeight = FontWeight.Bold)
                    Text(
                        "1) तुमचा दुसरा नंबर किंवा मित्राचा नंबर VIP मध्ये टाका.\n" +
                            "2) फोन Silent किंवा Vibrate वर ठेवा.\n" +
                            "3) त्या नंबरवरून कॉल करायला सांगा — VIP रिंगटोन पूर्ण आवाजात वाजेल.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        item {
            TextButton(onClick = actions.appSettings, modifier = Modifier.fillMaxWidth()) {
                Text("परवानगी नाकारली गेली असेल तर: App Settings उघडा")
            }
        }
    }
}

/** done = null म्हणजे स्टेटस तपासता येत नाही (Autostart) */
@Composable
private fun SetupItem(title: String, desc: String, done: Boolean?, button: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                when (done) {
                    true -> Icons.Filled.CheckCircle
                    false -> Icons.Filled.Warning
                    null -> Icons.Filled.Settings
                },
                null,
                tint = when (done) {
                    true -> MaterialTheme.colorScheme.tertiary
                    false -> MaterialTheme.colorScheme.error
                    null -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(desc, style = MaterialTheme.typography.bodySmall)
                if (done != true) {
                    Spacer(Modifier.size(6.dp))
                    FilledTonalButton(onClick = onClick) { Text(button) }
                } else {
                    Text("✓ पूर्ण", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// =====================================================================
// सेटिंग
// =====================================================================

@Composable
private fun SettingsScreen(
    settings: VipSettings,
    perms: PermStatus,
    ringing: Boolean,
    store: VipStore,
    onChanged: () -> Unit,
    onPickSystemTone: () -> Unit,
    onPickFile: () -> Unit,
    onClearTone: () -> Unit,
    onTest: () -> Unit,
    onStopTest: () -> Unit,
    onOpenDnd: () -> Unit
) {
    var volume by remember(settings.defaultVolume) { mutableFloatStateOf(settings.defaultVolume.toFloat()) }
    var maxRing by remember(settings.maxRingSeconds) { mutableFloatStateOf(settings.maxRingSeconds.toFloat()) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(vertical = 4.dp)) {
                SwitchRow("VIP CONTACT चालू", "बंद केल्यास कोणत्याही कॉलला ॲप आवाज करणार नाही", settings.enabled) {
                    store.enabled = it; onChanged()
                }
                HorizontalDivider()
                SwitchRow(
                    "फक्त Silent / Vibrate असताना वाजवा",
                    "बंद केल्यास Normal मोडमध्ये पण VIP टोन वाजेल",
                    settings.onlyWhenQuiet
                ) { store.onlyWhenQuiet = it; onChanged() }
                HorizontalDivider()
                SwitchRow(
                    "DND मध्ये पण वाजवा",
                    if (perms.dnd) "Do Not Disturb असतानाही VIP कॉलला आवाज" else "यासाठी DND परवानगी लागेल — टॅप करा",
                    settings.overrideDnd && perms.dnd
                ) {
                    if (!perms.dnd) onOpenDnd() else { store.overrideDnd = it; onChanged() }
                }
                HorizontalDivider()
                SwitchRow("व्हायब्रेशन", "रिंगटोनसोबत व्हायब्रेट पण होईल", settings.vibrate) {
                    store.vibrate = it; onChanged()
                }
                HorizontalDivider()
                SwitchRow("फ्लॅश लाईट ब्लिंक", "VIP कॉल आल्यावर टॉर्च चालू-बंद होईल", settings.flash) {
                    store.flash = it; onChanged()
                }
                HorizontalDivider()
                SwitchRow(
                    "रिपीट कॉलर (इमर्जन्सी)",
                    "VIP नसलेल्या नंबरने 3 मिनिटांत दुसऱ्यांदा कॉल केला तर तो पण वाजेल",
                    settings.repeatCaller
                ) { store.repeatCaller = it; onChanged() }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("डिफॉल्ट VIP रिंगटोन", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "ज्या VIP ला वेगळा टोन दिला नाही त्यांना हा वाजेल",
                    style = MaterialTheme.typography.bodySmall
                )
                Text("🎵 " + (settings.defaultRingtoneName ?: "फोनचा रिंगटोन"), fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onPickSystemTone, modifier = Modifier.weight(1f)) { Text("फोन टोन") }
                    OutlinedButton(onClick = onPickFile, modifier = Modifier.weight(1f)) { Text("MP3 फाईल") }
                }
                if (settings.defaultRingtoneUri != null) {
                    TextButton(onClick = onClearTone) { Text("फोनचा रिंगटोन वापरा") }
                }
                Text("आवाज: ${volume.roundToInt()}%")
                Slider(
                    value = volume,
                    onValueChange = { volume = it },
                    onValueChangeFinished = { store.defaultVolume = volume.roundToInt(); onChanged() },
                    valueRange = 10f..100f,
                    steps = 8
                )
                Text("जास्तीत जास्त वाजण्याचा वेळ: ${maxRing.roundToInt()} सेकंद")
                Slider(
                    value = maxRing,
                    onValueChange = { maxRing = it },
                    onValueChangeFinished = { store.maxRingSeconds = maxRing.roundToInt(); onChanged() },
                    valueRange = 30f..120f,
                    steps = 5
                )
                TestButton(ringing, onTest, onStopTest)
            }
        }

        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("VIP CONTACT • Version 1.0", fontWeight = FontWeight.Bold)
                Text("Ganesha Digital, Surgana (Nashik)", style = MaterialTheme.typography.bodyMedium)
                Text("contact.ganeshadigital@gmail.com", style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.size(24.dp))
    }
}

@Composable
private fun SwitchRow(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun TestButton(ringing: Boolean, onTest: () -> Unit, onStop: () -> Unit) {
    if (ringing) {
        Button(
            onClick = onStop,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Filled.Close, null)
            Spacer(Modifier.width(6.dp))
            Text("टेस्ट थांबवा")
        }
    } else {
        FilledTonalButton(onClick = onTest, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.PlayArrow, null)
            Spacer(Modifier.width(6.dp))
            Text("टेस्ट वाजवून पहा (10 सेकंद)")
        }
    }
}

// =====================================================================
// VIP कॉन्टॅक्ट जोडा / बदला
// =====================================================================

@Composable
private fun EditContactDialog(
    draft: VipContact,
    isNew: Boolean,
    defaultToneName: String,
    ringing: Boolean,
    onChange: (VipContact) -> Unit,
    onPickSystemTone: () -> Unit,
    onPickFile: () -> Unit,
    onTest: () -> Unit,
    onStopTest: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val valid = PhoneUtil.normalize(draft.number).length >= 5
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Star, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(if (isNew) "नवीन VIP कॉन्टॅक्ट" else "VIP कॉन्टॅक्ट बदला") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = { onChange(draft.copy(name = it)) },
                    label = { Text("नाव") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.number,
                    onValueChange = { onChange(draft.copy(number = it)) },
                    label = { Text("मोबाईल नंबर") },
                    leadingIcon = { Icon(Icons.Filled.Phone, null) },
                    singleLine = true,
                    isError = draft.number.isNotEmpty() && !valid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("रिंगटोन", style = MaterialTheme.typography.labelLarge)
                Text(
                    "🎵 " + (draft.ringtoneName ?: "डिफॉल्ट ($defaultToneName)"),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onPickSystemTone, modifier = Modifier.weight(1f)) { Text("फोन टोन") }
                    OutlinedButton(onClick = onPickFile, modifier = Modifier.weight(1f)) { Text("MP3 फाईल") }
                }
                if (draft.ringtoneUri != null) {
                    TextButton(onClick = { onChange(draft.copy(ringtoneUri = null, ringtoneName = null)) }) {
                        Text("डिफॉल्ट टोन वापरा")
                    }
                }
                Text("आवाज: ${draft.volume}%", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = draft.volume.toFloat(),
                    onValueChange = { onChange(draft.copy(volume = it.roundToInt())) },
                    valueRange = 10f..100f,
                    steps = 8
                )
                TestButton(ringing, onTest, onStopTest)
                if (!isNew) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Filled.Delete, null)
                        Spacer(Modifier.width(6.dp))
                        Text("VIP यादीतून काढा")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onSave, enabled = valid) { Text("सेव्ह करा") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("रद्द") }
        }
    )
}
