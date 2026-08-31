# Next.js to Android Hybrid Wrapper & CI/CD Deployment Guide

This project is a native Android wrapper powered by a **Capacitor JavaScript Bridge** that packages your Next.js application into a performant Android app while retaining all web features and exposing native device capabilities (Camera, Geolocation, Haptics, Local Storage, Network, and System Telemetry).

---

## 1. Connecting Your GitHub Repository (Two-Way Sync)

1. In the **AI Studio** interface, click on the **Sync / GitHub** icon in the upper right navigation bar.
2. Authorize GitHub and select your repository.
3. Every commit pushed to GitHub will automatically sync with this project, and changes made in AI Studio will sync back to your GitHub repository.

---

## 2. Automated GitHub Actions CI/CD Pipeline

The repository includes a ready-to-run continuous deployment workflow at `.github/workflows/deploy-android.yml`.

### Workflow Triggers:
- **Every push** to `main` or `master` branches
- **Version tags** (e.g. `v1.0.0`, `v1.0.1`)
- **Manual Trigger** (`workflow_dispatch`) via GitHub Actions tab

### Automated Steps:
1. **Checkout Code**: Fetches latest codebase with git history.
2. **Next.js Static Export**: Builds `npm run build` and exports web assets to `app/src/main/assets/www`.
3. **Java & Gradle Setup**: Configures JDK 17 and Gradle build environment.
4. **Automated Testing**: Runs unit tests (`gradle testDebugUnitTest`).
5. **Compile APKs & Bundle**: Produces `app-debug.apk`, `app-release.apk`, and `bundleRelease.aab` for Google Play Store.
6. **Publish Release**: Generates a GitHub Release with attached downloadable APK assets for instant device installation.

---

## 3. Configuring GitHub Secrets for Release Signing (Optional)

To automatically sign release APKs in GitHub Actions, configure the following secrets in **GitHub Repo Settings -> Secrets and variables -> Actions**:

| Secret Name | Description |
|---|---|
| `KEYSTORE_BASE64` | Base64-encoded `.jks` release keystore file (`base64 -w 0 my-upload-key.jks`) |
| `STORE_PASSWORD` | Keystore store password |
| `KEY_PASSWORD` | Key alias password |
| `KEY_ALIAS` | Key alias name (default: `upload`) |

*(If secrets are omitted, the pipeline will still generate debug APKs ready for installation).*

---

## 4. Next.js Configuration for Static Export

In your `next.config.js` or `next.config.mjs`, set:

```javascript
/** @type {import('next').NextConfig} */
const nextConfig = {
  output: 'export', // Enables static HTML/CSS/JS export to 'out' directory
  images: {
    unoptimized: true // Required for local file:// Android assets
  }
};

module.exports = nextConfig;
```

Then run:
```bash
npm run build
cp -r out/* app/src/main/assets/www/
```

---

## 5. Capacitor JavaScript Bridge API

Your Next.js React components can access native hardware APIs without any third-party SDK dependencies via `window.Capacitor` or `window.AndroidBridge`:

```javascript
// Native Haptics & Vibration
window.Capacitor.toNative('Haptics', 'impact', { style: 'heavy' });

// Native Toast
window.Capacitor.toNative('Toast', 'show', { text: 'Hello from Next.js!' });

// Camera Capture
window.Capacitor.toNative('Camera', 'getPhoto', { source: 'camera' }, 'my_callback_id');

// GPS Geolocation
window.Capacitor.toNative('Geolocation', 'getCurrentPosition', {}, 'geo_callback_id');

// Persistent Native Storage
window.Capacitor.toNative('Preferences', 'set', { key: 'token', value: '12345' });
```
