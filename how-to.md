# How-To Guide & Cookbook: Next.js Capacitor Android App

This guide provides practical recipes, code snippets, and instructions for common development tasks when working with this Next.js Capacitor Android hybrid application.

---

## 📑 Table of Contents

1. [How to Switch Between Local Assets, Dev Server, and Production URL](#1-how-to-switch-between-urls)
2. [How to Use Native Capacitor Bridge APIs in React / Next.js](#2-how-to-use-native-apis-in-react--nextjs)
   - [Biometric Authentication (Fingerprint / Face ID)](#biometric-authentication)
   - [Real-Time Network Monitoring](#network-monitoring)
   - [Camera & Photo Library](#camera--photo-library)
   - [GPS Geolocation](#gps-geolocation)
   - [Haptic Feedback & Vibration](#haptic-feedback)
   - [Native Persistent Key-Value Storage](#native-storage)
   - [System Clipboard & Native Sharing](#clipboard--native-sharing)
   - [Native Toasts](#native-toasts)
3. [How to Create a React Hook for Native APIs](#3-how-to-create-a-react-hook)
4. [How to Add a New Native Method to the Capacitor Bridge](#4-how-to-add-a-new-native-method)
5. [How to Debug the Web Application & Native Bridge](#5-how-to-debug)
6. [How to Build and Release a New APK via GitHub Actions](#6-how-to-build-and-release)

---

## 1. How to Switch Between URLs

The application can load your Next.js application from three different source modes:

1. **Embedded Local Bundle (`file:///android_asset/www/index.html`)**: Fully offline; loads the static files exported from your Next.js project.
2. **Local Dev Server (`http://10.0.2.2:3000` or your LAN IP)**: Allows instant Hot Module Replacement (HMR) while testing native bridge APIs.
   > *Note: On Android emulators, `10.0.2.2` aliases `localhost` on the development computer.*
3. **Live Deployed URL (`https://your-domain.com`)**: Connects to your staging or production Next.js web application.

### Method A: Using the In-App Management Hub (Runtime)
1. Tap the floating **Settings / Hub** button at the top-right of the screen.
2. Select one of the preset buttons (**Local Assets**, **Local Dev 3000**, **Local Dev 8080**, or **Production**), or type a custom URL into the input field.
3. Tap **Load URL**. The WebView reloads immediately.

### Method B: Setting the Default URL in Code
To change the default starting URL when the app boots, update `AppViewModel.kt`:

```kotlin
// app/src/main/java/com/example/viewmodel/AppViewModel.kt
class AppViewModel : ViewModel() {
    private val _currentUrl = MutableStateFlow("https://your-production-app.com")
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()
    // ...
}
```

---

## 2. How to Use Native APIs in React / Next.js

All native device APIs are accessible via `window.Capacitor.Plugins` and fallback gracefully if running in a standard web browser.

### Biometric Authentication

Prompt for native Android `BiometricPrompt` (Fingerprint or Face Unlock) and check hardware capability:

```javascript
// 1. Check if device has enrolled biometrics
const biometry = await window.Capacitor.Plugins.BiometricAuth.checkBiometry();
console.log('Biometrics available:', biometry.isAvailable); // true / false
console.log('Status description:', biometry.status);

// 2. Prompt user for biometric scan
try {
  const result = await window.Capacitor.Plugins.BiometricAuth.authenticate({
    title: 'Confirm Payment',
    subtitle: 'Verify your identity to proceed',
    description: 'Scan fingerprint or face',
    cancelTitle: 'Cancel'
  });

  if (result.success || result.verified) {
    console.log('Biometric identity confirmed!');
    // Proceed with secure action
  }
} catch (error) {
  console.warn('Biometric authentication failed or cancelled:', error.message);
}

// 3. Save / Get secure lock preference
await window.Capacitor.Plugins.BiometricAuth.setSecureAppAccess({ enabled: true });
const secureStatus = await window.Capacitor.Plugins.BiometricAuth.getSecureAppAccess();
```

---

### Network Monitoring

Listen to real-time network status changes (WiFi, Cellular, Offline):

```javascript
// Query current connection
const status = await window.Capacitor.Plugins.Network.getStatus();
console.log('Connected:', status.connected);
console.log('Connection Type:', status.connectionType); // 'wifi' | 'cellular' | 'none'

// Subscribe to real-time changes
const removeListener = window.Capacitor.Plugins.Network.addListener('networkStatusChange', (status) => {
  console.log('Network changed:', status.connectionType, 'Online:', status.connected);
});

// To clean up:
// removeListener();
```

---

### Camera & Photo Library

Capture a photo using the Android Camera or pick an existing image from the gallery:

```javascript
// Take a photo with the camera
const photo = await window.Capacitor.Plugins.Camera.getPhoto({
  source: 'CAMERA', // 'CAMERA' or 'PHOTOS'
  quality: 85
});

// The result is a Base64 data URL ready for <img> tags
const imageElement = document.getElementById('preview');
imageElement.src = photo.dataUrl; // data:image/jpeg;base64,...
```

---

### GPS Geolocation

Retrieve high-accuracy device GPS coordinates:

```javascript
try {
  const position = await window.Capacitor.Plugins.Geolocation.getCurrentPosition({
    enableHighAccuracy: true,
    timeout: 10000
  });

  const { latitude, longitude, altitude, accuracy } = position.coords;
  console.log(`GPS: Lat ${latitude.toFixed(5)}, Lng ${longitude.toFixed(5)} (±${accuracy}m)`);
} catch (err) {
  console.error('Location error:', err.message);
}
```

---

### Haptic Feedback

Trigger subtle or distinct vibration impulses for interactive elements:

```javascript
// Types: 'light', 'medium', 'heavy'
await window.Capacitor.toNative('Haptics', 'impact', { style: 'heavy' });

// Simple vibration with duration in ms
await window.Capacitor.toNative('Haptics', 'vibrate', { duration: 200 });
```

---

### Native Storage

Store and retrieve persistent data in Android's native `SharedPreferences`:

```javascript
// Save data
await window.Capacitor.toNative('Storage', 'set', {
  key: 'user_auth_token',
  value: 'xyz987654321'
});

// Retrieve data
const data = await window.Capacitor.toNative('Storage', 'get', {
  key: 'user_auth_token'
});
console.log('Stored token:', data.value);
```

---

### Clipboard & Native Sharing

```javascript
// Copy text to Android clipboard
await window.Capacitor.toNative('Clipboard', 'write', {
  string: 'https://myapp.com/invite/123'
});

// Open Android System Share Sheet
await window.Capacitor.toNative('Share', 'share', {
  title: 'Check out this app!',
  text: 'Here is an exciting update from my Next.js mobile app.',
  url: 'https://myapp.com'
});
```

---

### Native Toasts

Show standard Android toast messages:

```javascript
await window.Capacitor.toNative('Toast', 'show', {
  text: 'Saved successfully!',
  duration: 'short' // 'short' or 'long'
});
```

---

## 3. How to Create a React Hook for Native APIs

Here is a ready-to-use custom React hook (`useCapacitor.ts`) for your Next.js project:

```typescript
// hooks/useCapacitor.ts
import { useState, useEffect } from 'react';

export function useNativeNetwork() {
  const [network, setNetwork] = useState({ connected: true, connectionType: 'unknown' });

  useEffect(() => {
    if (typeof window === 'undefined' || !window.Capacitor?.Plugins?.Network) return;

    // Get initial state
    window.Capacitor.Plugins.Network.getStatus().then(setNetwork);

    // Listen to changes
    const unsub = window.Capacitor.Plugins.Network.addListener('networkStatusChange', (status: any) => {
      setNetwork(status);
    });

    return () => {
      if (typeof unsub === 'function') unsub();
    };
  }, []);

  return network;
}
```

---

## 4. How to Add a New Native Method to the Capacitor Bridge

To add a new native feature (e.g. Flashlight, Battery, or NFC):

1. Open `app/src/main/java/com/example/bridge/CapacitorBridge.kt`.
2. Add your plugin handler in `handleAction()`:

```kotlin
"Battery" -> {
    when (method) {
        "getStatus" -> {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val isCharging = bm.isCharging
            val result = JSONObject().apply {
                put("level", level)
                put("isCharging", isCharging)
            }
            sendCallback(callbackId, result)
            return result.toString()
        }
    }
}
```

3. Call your new method in your Next.js JavaScript code:
```javascript
const battery = await window.Capacitor.toNative('Battery', 'getStatus');
console.log(`Battery: ${battery.level}%, Charging: ${battery.isCharging}`);
```

---

## 5. How to Debug

### Chrome DevTools Remote Debugging
1. Connect your Android device or start an emulator.
2. Open Google Chrome on your computer and navigate to:
   ```
   chrome://inspect/#devices
   ```
3. Locate your Next.js application under the device list and click **Inspect**.
4. You have full access to the Web Console, Network tab, Elements tree, and JavaScript source breakpoints.

### In-App Bridge Event Stream
1. Tap the floating **Settings / Hub** icon at the top-right.
2. Scroll to the **Native Bridge Diagnostics & Log Stream** section.
3. Review all live calls exchanged between Next.js and Kotlin in real-time.

---

## 6. How to Build and Release via GitHub Actions

### Automatic Release on Tag Push
To trigger an automated release build with generated APK and AAB binaries:

```bash
git tag v1.0.0
git push origin v1.0.0
```

1. The `.github/workflows/deploy-android.yml` workflow triggers automatically.
2. It compiles the project, runs unit tests, generates `app-debug.apk` and release artifacts.
3. A new GitHub Release is created automatically with the compiled APK attached as a downloadable asset.
