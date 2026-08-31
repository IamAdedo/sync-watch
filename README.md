# Next.js Capacitor Android Hybrid Application

A high-performance native Android container and wrapper engineered to transform any **Next.js** web application into a native mobile application while retaining full web functionality and exposing native hardware capabilities through a **Capacitor JavaScript Bridge**.

Includes automated **GitHub Actions CI/CD workflows** for two-way synchronization, automated builds, and instant APK releases on every git push.

---

## 📱 Features & Capabilities

- **Native Next.js Web Shell**: Loads your Next.js application either from an offline embedded bundle (`file:///android_asset/www/index.html`), a local development server (`http://10.0.2.2:3000`), or a live deployed staging/production URL.
- **Capacitor JavaScript Bridge (`window.Capacitor` / `window.AndroidBridge`)**: Exposes native device APIs directly to your React / Next.js code without modifying core web logic:
  - 📷 **Camera & Gallery**: Photo capture and file picking with base64 data URL streaming.
  - 📍 **GPS Geolocation**: Real-time GPS coordinates, altitude, and precision tracking.
  - 📳 **Haptics & Vibration**: Haptic feedback patterns (light, medium, heavy impact clicks).
  - 💾 **Persistent Storage**: Native key-value persistence via Android `SharedPreferences`.
  - 📡 **Network & Telemetry**: Live connection type (WiFi, Cellular) and device specifications.
  - 📋 **System Clipboard & Native Sharing**: Direct access to Android clipboard and share sheet.
  - 🔔 **Native Toasts**: Android system toast notifications.
- **Management Hub & Diagnostics**: Built-in Jetpack Compose modal bottom sheet for quick URL switching, bridge event stream debugging, and pipeline setup.
- **Elegant Dark Theme**: Polished Material 3 dark design with deep slate backgrounds, lavender accents, and responsive layout scaling.

---

## 🚀 Quick Start & Integration

### 1. Export Your Next.js Web Application

In your Next.js project (`next.config.js` or `next.config.mjs`), enable static HTML export:

```javascript
/** @type {import('next').NextConfig} */
const nextConfig = {
  output: 'export',
  images: {
    unoptimized: true // Necessary for local file:// Android assets
  }
};

module.exports = nextConfig;
```

Export and copy the web assets to the Android bundle directory:

```bash
npm run build
cp -r out/* app/src/main/assets/www/
```

*(Alternatively, use the included helper script: `./scripts/sync_nextjs_assets.sh`)*

---

### 2. Using the Capacitor Bridge in Next.js / React

Access native device APIs directly from any React component:

```javascript
// Real-Time Network Connectivity Listener
const networkListener = window.Capacitor.Plugins.Network.addListener('networkStatusChange', (status) => {
  console.log('Network connected:', status.connected, 'Type:', status.connectionType);
  // status.connectionType -> 'wifi' | 'cellular' | 'ethernet' | 'none'
});

// Query Current Network Status
const status = await window.Capacitor.Plugins.Network.getStatus();
console.log('Current connection:', status.connectionType);

// Native Biometric Auth Prompt (Fingerprint / Face ID)
const bioResult = await window.Capacitor.Plugins.BiometricAuth.authenticate({
  title: 'Secure App Access',
  subtitle: 'Confirm your fingerprint or face scan',
  reason: 'Authenticate to access sensitive account data'
});
console.log('Biometric scan verified:', bioResult.verified);

// Check Biometric Hardware Support
const bioStatus = await window.Capacitor.Plugins.BiometricAuth.checkBiometry();
console.log('Biometric available:', bioStatus.isAvailable, bioStatus.status);

// Native Haptic Click
window.Capacitor.toNative('Haptics', 'impact', { style: 'heavy' });

// Native Toast
window.Capacitor.toNative('Toast', 'show', { text: 'Hello from Next.js!' });

// Camera Capture
window.Capacitor.toNative('Camera', 'getPhoto', { source: 'camera' }, 'photo_cb');

// Geolocation
window.Capacitor.toNative('Geolocation', 'getCurrentPosition', {}, 'geo_cb');

// Persistent Storage
window.Capacitor.toNative('Preferences', 'set', { key: 'auth_token', value: 'xyz123' });
```

---

## 🔄 Two-Way GitHub Sync & CI/CD Deployment

### Connecting to GitHub
1. Click the **GitHub / Sync** button in the top right menu of Google AI Studio.
2. Select your repository to enable two-way synchronization between AI Studio and GitHub.

### Automated GitHub Actions Workflow (`.github/workflows/deploy-android.yml`)
Every push to `main` or `master` (or git release tags `v*`) automatically executes:
1. **Next.js Static Build**: Compiles web assets (`npm run build`) and copies output to `app/src/main/assets/www/`.
2. **Automated Unit Testing**: Runs local JVM test suites (`gradle testDebugUnitTest`).
3. **APK & AAB Compilation**: Generates both Debug and Release APKs and Google Play Android App Bundles (`.aab`).
4. **GitHub Releases**: Creates a new GitHub Release with attached downloadable `.apk` files ready for direct installation on Android devices.

---

## 🔐 Release Signing Configuration (Optional)

To enable automatic release signing in GitHub Actions, add these repository secrets in **GitHub -> Settings -> Secrets and variables -> Actions**:

| Secret Name | Description |
|---|---|
| `KEYSTORE_BASE64` | Base64-encoded release `.jks` keystore file |
| `STORE_PASSWORD` | Keystore password |
| `KEY_PASSWORD` | Key alias password |
| `KEY_ALIAS` | Key alias name (default: `upload`) |

---

## 🛠 Project Structure

```
├── .github/workflows/
│   ├── deploy-android.yml         # Automated CI/CD build & APK release pipeline
│   └── verify-pr.yml              # Pull request verification workflow
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml    # App permissions (Camera, GPS, Internet)
│   │   ├── assets/www/            # Bundled Next.js web application files
│   │   ├── java/com/example/
│   │   │   ├── MainActivity.kt    # Android entry point
│   │   │   ├── bridge/            # Capacitor JavaScript Bridge implementation
│   │   │   ├── ui/                # Jetpack Compose WebScreen & Management Hub
│   │   │   └── viewmodel/         # App UI state & URL navigation logic
│   └── build.gradle.kts           # Android Gradle configuration
├── scripts/
│   ├── build_release_apk.sh       # Local build script for APKs
│   └── sync_nextjs_assets.sh      # Web asset synchronization script
└── CI_CD_GUIDE.md                 # Detailed CI/CD reference guide
```
