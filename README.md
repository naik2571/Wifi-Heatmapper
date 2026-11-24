# Wi-Fi Heatmapper

> **Real-time RF signal strength (RSSI), channel distribution, and spatial heatmapping scanner.**

An enterprise-grade, privacy-first Android application developed in modern Kotlin and Jetpack Compose/Views. Built with strict adherence to modern Android architecture, lifecycle awareness, and hardware sensor efficiency.

---

## 🚀 Key Features

- Continuous Wi-Fi RSSI signal polling (-100 dBm to -30 dBm) with live signal graph.
- Frequency band differentiation (2.4 GHz vs 5 GHz vs 6 GHz Wi-Fi 6).
- Color-coded signal quality indicators (Excellent / Good / Fair / Poor).
- Spatial walk-testing trail for identifying Wi-Fi dead zones in homes and offices.

---

## 🛠️ Hardware & Sensor Architecture

* **Sensors & APIs:** `WifiManager, LocationManager (for Wi-Fi beacon scanning), TelephonyManager`
* **Declared Permissions:** `ACCESS_FINE_LOCATION, ACCESS_WIFI_STATE, CHANGE_WIFI_STATE`
* **Target SDK:** Android 14 (API 34)
* **Minimum SDK:** Android 8.0 (API 26+)
* **Architecture:** MVVM / Unidirectional Data Flow (UDF) with Coroutines & StateFlow

---

## 🔒 Privacy & Permission Transparency

This application is engineered under the **Zero Unnecessary Permissions** doctrine:
* **No Hidden Trackers:** Contains zero advertising SDKs or third-party behavioral analytics.
* **No Accessibility Privilege Misuse:** All background operations utilize sanctioned Android APIs (`WorkManager`, `ForegroundService`, `JobScheduler`).
* **Air-Gapped Operation:** Runs locally on your device hardware without requiring external cloud servers.

---

## 📦 Building and Running

### Prerequisites
* Android Studio Iguana / Jellyfish / Ladybug or newer
* Android SDK 34
* JDK 17+

### Build APK via CLI
```bash
# Clone the repository
git clone https://github.com/naik2571/Wifi-Heatmapper.git
cd Wifi-Heatmapper

# Build Debug APK
./gradlew assembleDebug

# Install on connected device via ADB
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 License
Licensed under the [Apache License 2.0](LICENSE).
