# VIP CONTACT ⭐

**Ganesha Digital** — Android app. Phone Silent / Vibrate / DND var asla tari VIP contact cha call aala ki tyacha swatahacha ringtone full awajat vajto.

- Package: `com.ganeshadigital.vipcontact`
- Android 8.0 (API 26) te Android 15 (API 35)
- Kotlin + Jetpack Compose, Marathi UI

---

## 1. APK kasa banvaycha (GitHub — sopa marg, PC chi garaj nahi)

1. GitHub var navin **Private** repository banva, naav `vip-contact`.
   (Private thevaa — yamadhe app chi signing key aahe.)
2. Ha sagla folder (zip extract karun) tya repo madhe upload kara.
   - Web var: **Add file → Upload files** → sagle files/folders drag kara → **Commit**.
   - `.github` folder pan upload zala pahije (hidden folder aahe, dakhvle nasel tar Git ne push kara).
3. Repo madhe **Actions** tab → **Build VIP CONTACT APK** → 4–6 minitat build hoil.
4. Build hirva ✓ zala ki tyavar click kara → khali **Artifacts** madhe **VIP-CONTACT-APK** download kara.
5. Zip ughada → `VIP-CONTACT-v1.0.apk` phone var install kara.

Code badalun parat push kela ki navin APK automatic banto.

## 2. Android Studio madhun (PC var)

1. Android Studio (Ladybug kiva navin) → **Open** → ha `VIPContact` folder.
2. Gradle sync zala ki **Build → Build App Bundle(s)/APK(s) → Build APK(s)**.
3. Release APK: terminal madhe `./gradlew assembleRelease` → `app/build/outputs/apk/release/`.

## 3. Signing key (MAHATVACHE)

- `vipcontact.jks` ani `keystore.properties` hi app chi kayamchi key aahe.
- Hich key vaparli tarach juna app update hoto. **Hi file harvu naka, backup Google Drive var theva.**
- Password: `GaneshaVip2026` (alias `vipcontact`).

## 4. Phone var setup (ekdach)

App ughadla ki **सेटअप** tab madhe kram-kramane:

| Payri | Ka lagte |
|---|---|
| फोन स्टेट परवानगी | Call uchalla / kat zala ki ringtone band |
| नोटिफिकेशन (Android 13+) | "Ringtone band kara" button |
| Caller ID app (Android 10+) | Yenara number olakhnyasathi |
| कॉल लॉग (पर्यायी) | Android 9 kiva Truecaller vaparat asal tar |
| Do Not Disturb | DND madhe pan vajnyasathi |
| बॅटरी ऑप्टिमायझेशन बंद | App background madhe band hou naye |
| Autostart | Mi / Redmi / Vivo / Oppo / Realme sathi |

**Samsung:** Settings → Apps → VIP CONTACT → Battery → **Unrestricted**.

## 5. Kasa kam karto

1. Call aala → Android 10+ var `CallScreeningService` number deto (Android 8–9 var `PHONE_STATE` + Call Log).
2. Number VIP yadit asel ani phone Silent/Vibrate/DND var asel tar →
   ringtone **ALARM stream** var vajto (silent mode alarm la thambvat nahi).
3. Alarm volume tatpurta contact cha % la set hoto, DND "Alarms only" var jato.
4. Call uchalla / kat kela / miss zala / notification button / time sampla → ringtone band, volume ani DND parat purvi sarkhe.

Extra: व्हायब्रेशन, फ्लॅश ब्लिंक, रिपीट कॉलर (3 minitat dusra call), max ring time, Normal mode madhe pan vajvayche option.

## 6. Play Store var takaycha asel tar

- `READ_CALL_LOG` Play Store var restricted permission aahe. Play Store version sathi
  `AndroidManifest.xml` madhun `READ_CALL_LOG` line kadhun taka — Android 10+ var Caller ID role ne kam chalte.
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` sathi Play Console madhe karan dyave lagel.
- Direct APK (sideload) sathi kahi badal lagat nahi.

## 7. Files

```
app/src/main/java/com/ganeshadigital/vipcontact/
├── MainActivity.kt
├── core/RingController.kt        # ringtone + volume + DND + vibrate + flash
├── core/CallHandler.kt           # VIP aahe ka, vajvayche ka — nirnay
├── data/VipStore.kt              # VIP yadi + settings
├── service/VipCallScreeningService.kt
├── service/CallStateReceiver.kt
├── service/RingService.kt        # foreground notification
├── service/StopReceiver.kt
├── ui/VipApp.kt                  # VIP यादी, सेटअप, सेटिंग screens
├── ui/Theme.kt
└── util/ (Perms, PhoneUtil, Tones)
```

Ganesha Digital, Surgana (Nashik) • contact.ganeshadigital@gmail.com
