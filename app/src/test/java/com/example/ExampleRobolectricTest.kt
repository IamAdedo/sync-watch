package com.example

import android.content.Context
import android.webkit.WebView
import androidx.test.core.app.ApplicationProvider
import com.example.bridge.CapacitorBridge
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Next.js Capacitor App", appName)
  }

  @Test
  fun `capacitor bridge network getStatus returns valid json`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val webView = WebView(context)
    val bridge = CapacitorBridge(context, webView)

    val networkStatus = bridge.getCurrentNetworkStatus()
    assertNotNull(networkStatus)
    assertTrue(networkStatus.has("connected"))
    assertTrue(networkStatus.has("connectionType"))

    val resultJson = bridge.handleAction("Network", "getStatus", "{}", "test_cb")
    assertNotNull(resultJson)
    val parsed = JSONObject(resultJson!!)
    assertTrue(parsed.has("connected"))
    assertTrue(parsed.has("connectionType"))

    bridge.stopNetworkMonitoring()
  }

  @Test
  fun `capacitor bridge biometric auth actions return valid json and update prefs`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val webView = WebView(context)
    val bridge = CapacitorBridge(context, webView)

    // Check biometry status
    val bioStatus = bridge.checkBiometryStatus()
    assertNotNull(bioStatus)
    assertTrue(bioStatus.has("isAvailable"))
    assertTrue(bioStatus.has("status"))
    assertTrue(bioStatus.has("secureAppAccess"))

    // Test setSecureAppAccess
    val setResult = bridge.handleAction("BiometricAuth", "setSecureAppAccess", """{"enabled": true}""", "test_cb_bio")
    assertNotNull(setResult)
    val parsedSet = JSONObject(setResult!!)
    assertTrue(parsedSet.optBoolean("enabled", false))

    // Test getSecureAppAccess
    val getResult = bridge.handleAction("BiometricAuth", "getSecureAppAccess", "{}", "test_cb_bio_get")
    assertNotNull(getResult)
    val parsedGet = JSONObject(getResult!!)
    assertTrue(parsedGet.optBoolean("enabled", false))

    bridge.stopNetworkMonitoring()
  }
}
