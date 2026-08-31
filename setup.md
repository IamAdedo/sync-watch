# Setup Guide: Next.js Capacitor Android Hybrid Application

This guide walks you through setting up, configuring, and building the Next.js Capacitor Android Hybrid Application from scratch on your local development machine and in CI/CD environments.

---

## 📋 Prerequisites

Before starting, ensure you have the following installed on your machine:

1. **Java Development Kit (JDK)**: JDK 17 or higher (recommended: OpenJDK 17 or Eclipse Temurin 17).
2. **Android SDK / Android Studio**:
   - Android SDK Platform 34 or higher
   - Android SDK Build-Tools (34.0.0+)
   - Android SDK Command-line Tools & Platform-Tools (`adb`)
3. **Node.js & npm / yarn / pnpm**: Node.js 18+ for building your Next.js application.
4. **Git**: For version control and CI/CD triggers.

---

## 📂 Project Structure

```
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── assets/www/          # Embedded Next.js exported web assets (HTML/CSS/JS)
│   │   │   ├── java/com/example/
│   │   │   │   ├── MainActivity.kt  # Android entry activity (FragmentActivity)
│   │   │   │   ├── bridge/          # CapacitorBridge & native plugin router
│   │   │   │   ├── ui/              # Jetpack Compose UI (WebView screen, Hub bottom sheet)
│   │   │   │   └── viewmodel/       # App state management & URL configurations
│   │   │   ├── res/                 # Android resources (icons, themes, strings)
│   │   │   └── AndroidManifest.xml  # System permissions & hardware features
│   │   └── test/                    # Robolectric & JVM unit tests
│   └── build.gradle.kts             # App-level build configurations & dependencies
├── scripts/
│   ├── build_release_apk.sh         # Helper script for building release APKs
│   └── sync_nextjs_assets.sh        # Sync script to copy Next.js 'out/' to Android assets
├── .github/workflows/
│   ├── deploy-android.yml           # Automated CI/CD pipeline for APK/AAB releases
│   └── verify-pr.yml                # PR validation and unit test workflow
├── gradle/libs.versions.toml        # Version Catalog for dependencies
├── settings.gradle.kts              # Root project settings
├── setup.md                         # This setup guide
└── how-to.md                        # Practical recipe and usage cookbook
```

---

## 🛠️ Step-by-Step Setup

### Step 1: Configure Your Next.js Project for Static Export

To embed your Next.js project into the Android APK for offline support, configure static export in your Next.js app's configuration file (`next.config.js` or `next.config.mjs`):

```javascript
/** @type {import('next').NextConfig} */
const nextConfig = {
  output: 'export',
  images: {
    unoptimized: true // Required for local file:/// Android asset URLs
  },
  trailingSlash: true // Ensures proper routing in local WebView environments
};

module.exports = nextConfig;
```

---

### Step 2: Build and Synchronize Next.js Assets

1. In your Next.js project directory, run the static export build:
   ```bash
   npm run build
   ```
   This generates an `out/` directory containing the standalone HTML, CSS, JavaScript, and asset files.

2. Copy the exported files into the Android project's asset directory:
   ```bash
   # From your project root:
   ./scripts/sync_nextjs_assets.sh /path/to/your/nextjs-app
   ```
   *Or manually copy the files:*
   ```bash
   rm -rf app/src/main/assets/www/*
   cp -r /path/to/your/nextjs-app/out/* app/src/main/assets/www/
   ```

---

### Step 3: Configure Android Environment & Build Settings

1. **Verify Environment Variables**:
   Ensure `ANDROID_HOME` or `ANDROID_SDK_ROOT` points to your Android SDK directory:
   ```bash
   export ANDROID_HOME=$HOME/Android/Sdk
   export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin
   ```

2. **Validate Gradle Build**:
   Test building the debug APK:
   ```bash
   gradle assembleDebug
   ```

3. **Run JVM & Robolectric Tests**:
   Verify that all native bridge components and unit tests pass:
   ```bash
   gradle testDebugUnitTest
   ```

---

### Step 4: Run on Emulator or Physical Device

#### Option A: Using Android Studio
1. Open the project folder in Android Studio.
2. Allow Gradle sync to complete.
3. Select an emulator or connected physical Android device.
4. Click **Run ('app')** (or press `Shift + F10`).

#### Option B: Using Command Line (`adb`)
1. Connect your Android device with USB Debugging enabled, or start an Android Virtual Device (AVD).
2. Install the debug APK:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
3. Launch the application:
   ```bash
   adb shell am start -n com.aistudio.nextjscapacitorapp.cbfyud/com.example.MainActivity
   ```

---

### Step 5: Setting Up CI/CD via GitHub Actions

The repository includes pre-configured GitHub Actions workflows in `.github/workflows/`:

- **`verify-pr.yml`**: Runs linting and unit tests on every Pull Request.
- **`deploy-android.yml`**: Builds debug APK, release APK, and App Bundle (AAB), creating a GitHub Release automatically on tags (`v*`) or pushes to `main`.

#### Optional: Configure GitHub Secrets for Signed Release Builds

If you want automated signed release APKs/AABs:
1. Navigate to your GitHub repository -> **Settings** -> **Secrets and variables** -> **Actions**.
2. Add the following repository secrets:
   - `KEYSTORE_BASE64`: Base64 encoded `.keystore` or `.jks` file (`base64 -w 0 your.keystore`).
   - `KEYSTORE_PASSWORD`: Keystore password.
   - `KEY_ALIAS`: Key alias name.
   - `KEY_PASSWORD`: Key password.

---

## 🎯 Verification Checklist

- [x] Next.js builds cleanly with `output: 'export'`.
- [x] Assets copied to `app/src/main/assets/www/`.
- [x] `gradle assembleDebug` succeeds without compilation errors.
- [x] `gradle testDebugUnitTest` runs green.
- [x] App launches and displays the Next.js UI with native Capacitor bridge functionality.
