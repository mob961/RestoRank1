# RestoRank Partner App - Android Build Instructions

## App Information
- **App Name:** RestoRank Partner
- **Package ID:** ai.restorank.partner
- **Version:** 1.0.0 (versionCode: 1)
- **Min Android:** 7.0 (API 24)
- **Target Android:** 14 (API 34)

## What This App Does
A native Android app that wraps the RestoRank Tables Dashboard and adds:
- **Thermal printer support** - Direct TCP printing to network printers (ESC/POS)
- **Real-time order notifications** - WebSocket connection for instant alerts
- **Auto-print** - Automatically print kitchen tickets when new orders arrive
- **Printer settings** - Configure printer IP, port, and auto-print preferences

---

## Build Methods

### Option 1: Build on Your Computer (Recommended)

#### Prerequisites
1. **Java JDK 17** - Download from [Adoptium](https://adoptium.net/)
2. **Android Studio** - Download from [developer.android.com](https://developer.android.com/studio)

#### Steps

1. **Clone/Download the project**
   ```bash
   # If using git
   git clone <your-repo-url>
   cd partner-app/android
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Click "Open" and select the `partner-app/android` folder
   - Wait for Gradle sync to complete (may take a few minutes first time)

3. **Build Debug APK**
   ```bash
   # From terminal in the android folder:
   ./gradlew assembleDebug
   
   # Or on Windows:
   gradlew.bat assembleDebug
   ```
   
   APK location: `app/build/outputs/apk/debug/app-debug.apk`

4. **Build Release APK** (for distribution)
   ```bash
   ./gradlew assembleRelease
   ```
   
   APK location: `app/build/outputs/apk/release/app-release-unsigned.apk`

---

### Option 2: Build with GitHub Actions (Cloud Build)

If you push this code to GitHub, the APK will be built automatically!

1. Push code to GitHub (main branch)
2. Go to **Actions** tab in your GitHub repo
3. Click on the latest workflow run
4. Download the APK from **Artifacts** section

The workflow is already configured in `.github/workflows/android-build.yml`

---

## Installing the APK on Your Phone

### Step-by-Step Sideload Instructions

1. **Enable Unknown Sources**
   - Go to **Settings > Security** (or **Settings > Apps > Special Access**)
   - Enable **Install unknown apps** for your file manager or browser
   - On newer Android: You'll be prompted when you try to install

2. **Transfer the APK**
   - **USB:** Connect phone to computer, copy APK to Downloads folder
   - **Cloud:** Upload to Google Drive, Dropbox, etc. and download on phone
   - **Direct:** Use ADB: `adb install app-debug.apk`

3. **Install the App**
   - Open your file manager
   - Navigate to where you saved the APK
   - Tap the APK file
   - Tap **Install** when prompted
   - If blocked, tap **Settings** and enable installation for that source

4. **Open the App**
   - Find "RestoRank Partner" in your app drawer
   - The app opens directly to the Tables Dashboard

---

## Setting Up Printing

1. **Open the app** on your Android phone
2. **Tap the gear icon** in the top-right corner (Printer Settings)
3. **Configure your printer:**
   - **Printer Name:** Kitchen (or whatever you prefer)
   - **Printer IP:** Your thermal printer's IP address (e.g., 192.168.68.50)
   - **Port:** Usually 9100
   - **Auto-print:** Enable to automatically print new orders
4. **Test Connection** - Verifies the printer is reachable
5. **Print Test Page** - Sends a test receipt to the printer
6. **Save Settings**

### Important: Network Requirements
- Your Android phone must be on the **same WiFi network** as the thermal printer
- The printer must have a static IP address
- Port 9100 must be accessible (default for most thermal printers)

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| "App not installed" | Enable unknown sources, check storage space |
| "Parse error" | APK may be corrupted, re-download |
| White screen | Check internet connection, pull to refresh |
| Can't find APK | Check Downloads folder or where you saved it |
| Printer not connecting | Check WiFi network, verify printer IP |
| Print test fails | Ensure printer is on and has paper |

---

## Features Included

- ✅ WebView wrapper loading Tables Dashboard
- ✅ Pull-to-refresh functionality
- ✅ Progress bar while loading
- ✅ Back button navigation within web app
- ✅ External links open in browser
- ✅ Network error handling with retry
- ✅ JavaScript and local storage enabled
- ✅ **Thermal printer support (ESC/POS over TCP)**
- ✅ **Printer settings screen**
- ✅ **Test connection & test print**
- ✅ **WebSocket for real-time order notifications**
- ✅ **Auto-print new orders**

---

## Need Help?

If you encounter issues building the app:
1. Ensure Java 17 is installed and in your PATH
2. Run `./gradlew --version` to verify Gradle works
3. Check Android Studio's SDK Manager has API 34 installed
