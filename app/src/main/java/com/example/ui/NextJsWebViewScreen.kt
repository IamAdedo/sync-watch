package com.example.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.example.bridge.CapacitorBridge
import com.example.bridge.WebChromeClientManager
import com.example.viewmodel.AppViewModel
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun NextJsWebViewScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var bridgeRef by remember { mutableStateOf<CapacitorBridge?>(null) }

    // File chooser callback holder for HTML file input
    var filePathCallbackHolder by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    // Pending camera callback for Capacitor Bridge
    var pendingCameraCallbackId by remember { mutableStateOf<String?>(null) }
    var tempCameraImageUri by remember { mutableStateOf<Uri?>(null) }

    // HTML File Chooser launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uris = if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            if (data?.clipData != null) {
                val count = data.clipData!!.itemCount
                (0 until count).map { data.clipData!!.getItemAt(it).uri }.toTypedArray()
            } else if (data?.data != null) {
                arrayOf(data.data!!)
            } else if (tempCameraImageUri != null) {
                arrayOf(tempCameraImageUri!!)
            } else null
        } else null
        filePathCallbackHolder?.onReceiveValue(uris)
        filePathCallbackHolder = null
    }

    // Camera launcher for Capacitor Bridge
    val takePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val cbId = pendingCameraCallbackId
        if (cbId != null) {
            if (success && tempCameraImageUri != null) {
                try {
                    val stream = context.contentResolver.openInputStream(tempCameraImageUri!!)
                    val bitmap = BitmapFactory.decodeStream(stream)
                    val out = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    val base64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                    val dataUrl = "data:image/jpeg;base64,$base64"

                    val json = JSONObject().apply {
                        put("dataUrl", dataUrl)
                        put("format", "jpeg")
                    }
                    bridgeRef?.sendCallback(cbId, json)
                    viewModel.addLog("Camera capture success -> sent to web app")
                } catch (e: Exception) {
                    bridgeRef?.sendError(cbId, e.message ?: "Failed to read captured image")
                    viewModel.addLog("Camera decode error: ${e.message}")
                }
            } else {
                bridgeRef?.sendError(cbId, "User cancelled photo capture")
                viewModel.addLog("Camera capture cancelled")
            }
            pendingCameraCallbackId = null
        }
    }

    // Gallery picker for Capacitor Bridge
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        val cbId = pendingCameraCallbackId
        if (cbId != null) {
            if (uri != null) {
                try {
                    val stream = context.contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(stream)
                    val out = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    val base64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                    val dataUrl = "data:image/jpeg;base64,$base64"

                    val json = JSONObject().apply {
                        put("dataUrl", dataUrl)
                        put("format", "jpeg")
                    }
                    bridgeRef?.sendCallback(cbId, json)
                    viewModel.addLog("Gallery image selected -> sent to web app")
                } catch (e: Exception) {
                    bridgeRef?.sendError(cbId, e.message ?: "Failed to load selected photo")
                }
            } else {
                bridgeRef?.sendError(cbId, "No image selected")
            }
            pendingCameraCallbackId = null
        }
    }

    // Permission launcher for Location / Camera
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        viewModel.addLog("Permissions updated: $permissions")
    }

    LaunchedEffect(Unit) {
        try {
            val requiredPermissions = arrayOf(
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
            val missingPermissions = requiredPermissions.filter { perm ->
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    perm
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            }.toTypedArray()

            if (missingPermissions.isNotEmpty()) {
                permissionLauncher.launch(missingPermissions)
            }
        } catch (e: Exception) {
            viewModel.addLog("Permission request skipped: ${e.message}")
        }
    }

    // Handle back button for in-webview browsing history
    BackHandler(enabled = uiState.canGoBack) {
        if (webViewRef?.canGoBack() == true) {
            webViewRef?.goBack()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.isConnected) Color(0xFF10B981) else Color(0xFFEF4444))
                            )
                            Text(
                                text = if (uiState.isEmbedded) "Next.js (Local Bundle)" else "Next.js (Live URL)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = uiState.currentUrl,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            webViewRef?.reload()
                            viewModel.addLog("Manual refresh triggered")
                        },
                        modifier = Modifier.testTag("reload_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reload Web View",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { viewModel.toggleSettingsSheet(true) },
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Open Settings and Deployment Hub",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("nextjs_webview"),
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewRef = this
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            allowFileAccess = true
                            allowContentAccess = true
                            mediaPlaybackRequiresUserGesture = false
                            cacheMode = WebSettings.LOAD_DEFAULT
                            setGeolocationEnabled(true)
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            userAgentString = "$userAgentString CapacitorHybrid/1.0 NextJsAndroid/1.0"
                        }

                        val bridge = CapacitorBridge(
                            context = ctx,
                            webView = this,
                            onLog = { logMsg -> viewModel.addLog(logMsg) },
                            onRequestPhoto = { source, callbackId ->
                                pendingCameraCallbackId = callbackId
                                if (source.lowercase() == "camera") {
                                    try {
                                        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                        val storageDir = ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                                        val photoFile = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
                                        val uri = FileProvider.getUriForFile(
                                            ctx,
                                            "${ctx.packageName}.fileprovider",
                                            photoFile
                                        )
                                        tempCameraImageUri = uri
                                        takePhotoLauncher.launch(uri)
                                    } catch (_: Exception) {
                                        // Fallback to gallery
                                        galleryLauncher.launch("image/*")
                                    }
                                } else {
                                    galleryLauncher.launch("image/*")
                                }
                            }
                        )
                        bridgeRef = bridge
                        addJavascriptInterface(bridge, "AndroidBridge")

                        webChromeClient = WebChromeClientManager(
                            onProgressUpdate = { progress ->
                                viewModel.setProgress(progress)
                            },
                            onConsoleLog = { consoleMsg ->
                                viewModel.addLog(consoleMsg)
                            },
                            onFileChooser = { callback, params ->
                                filePathCallbackHolder?.onReceiveValue(null)
                                filePathCallbackHolder = callback
                                try {
                                    val intent = params?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                                        type = "*/*"
                                    }
                                    filePickerLauncher.launch(intent)
                                    true
                                } catch (e: Exception) {
                                    filePathCallbackHolder = null
                                    false
                                }
                            }
                        )

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                viewModel.setProgress(15)
                                viewModel.setNavState(view?.canGoBack() == true, view?.canGoForward() == true)
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                viewModel.setProgress(100)
                                viewModel.setNavState(
                                    canGoBack = view?.canGoBack() == true,
                                    canGoForward = view?.canGoForward() == true,
                                    title = view?.title
                                )

                                // Inject Capacitor compatibility shim so all Next.js standard API calls route to AndroidBridge
                                val injectShim = """
                                    (function() {
                                        window.Capacitor = window.Capacitor || {};
                                        window.Capacitor.isNative = true;
                                        window.Capacitor.platform = 'android';
                                        window.Capacitor.Plugins = window.Capacitor.Plugins || {};
                                        window.Capacitor.toNative = window.Capacitor.toNative || function(plugin, method, options, callbackId) {
                                            if (window.AndroidBridge) {
                                                window.AndroidBridge.handleAction(plugin, method, JSON.stringify(options || {}), callbackId || '');
                                            }
                                        };
                                        if (!window.Capacitor.Plugins.Network) {
                                            window.Capacitor.Plugins.Network = {
                                                getStatus: function() {
                                                    return new Promise(function(resolve) {
                                                        if (window.AndroidBridge) {
                                                            var res = window.AndroidBridge.handleAction('Network', 'getStatus', '{}', '');
                                                            try { resolve(JSON.parse(res)); } catch(e) { resolve(res); }
                                                        } else {
                                                            resolve({ connected: navigator.onLine, connectionType: 'unknown' });
                                                        }
                                                    });
                                                },
                                                addListener: function(eventName, listener) {
                                                    if (window.Capacitor.addListener) {
                                                        return window.Capacitor.addListener('Network', eventName, listener);
                                                    }
                                                    return { remove: function() {} };
                                                }
                                            };
                                        }
                                        if (!window.Capacitor.Plugins.BiometricAuth) {
                                            window.Capacitor.Plugins.BiometricAuth = {
                                                checkBiometry: function() {
                                                    return new Promise(function(resolve) {
                                                        if (window.AndroidBridge) {
                                                            var res = window.AndroidBridge.handleAction('BiometricAuth', 'checkBiometry', '{}', '');
                                                            try { resolve(JSON.parse(res)); } catch(e) { resolve(res); }
                                                        } else {
                                                            resolve({ isAvailable: true, biometryType: 'fingerprint_or_face', status: 'Simulated' });
                                                        }
                                                    });
                                                },
                                                authenticate: function(options) {
                                                    return new Promise(function(resolve, reject) {
                                                        var cbId = 'bio_' + Date.now() + '_' + Math.random().toString(36).substr(2, 6);
                                                        window.__bioCallbacks = window.__bioCallbacks || {};
                                                        window.__bioCallbacks[cbId] = { resolve: resolve, reject: reject };
                                                        if (window.AndroidBridge) {
                                                            window.AndroidBridge.handleAction('BiometricAuth', 'authenticate', JSON.stringify(options || {}), cbId);
                                                        } else {
                                                            resolve({ success: true, verified: true });
                                                        }
                                                    });
                                                },
                                                setSecureAppAccess: function(options) {
                                                    return new Promise(function(resolve) {
                                                        if (window.AndroidBridge) {
                                                            var res = window.AndroidBridge.handleAction('BiometricAuth', 'setSecureAppAccess', JSON.stringify(options || {}), '');
                                                            try { resolve(JSON.parse(res)); } catch(e) { resolve(res); }
                                                        } else {
                                                            resolve({ success: true, enabled: !!options.enabled });
                                                        }
                                                    });
                                                },
                                                getSecureAppAccess: function() {
                                                    return new Promise(function(resolve) {
                                                        if (window.AndroidBridge) {
                                                            var res = window.AndroidBridge.handleAction('BiometricAuth', 'getSecureAppAccess', '{}', '');
                                                            try { resolve(JSON.parse(res)); } catch(e) { resolve(res); }
                                                        } else {
                                                            resolve({ enabled: false });
                                                        }
                                                    });
                                                }
                                            };
                                            window.Capacitor.Plugins.NativeBiometric = window.Capacitor.Plugins.BiometricAuth;
                                        }
                                        if (!window.Capacitor.Plugins.Preferences) {
                                            window.Capacitor.Plugins.Preferences = {
                                                set: function(options) {
                                                    return new Promise(function(resolve) {
                                                        if (window.AndroidBridge) {
                                                            var res = window.AndroidBridge.handleAction('Preferences', 'set', JSON.stringify(options || {}), '');
                                                            try { resolve(JSON.parse(res)); } catch(e) { resolve(res); }
                                                        } else {
                                                            resolve({ success: true });
                                                        }
                                                    });
                                                },
                                                get: function(options) {
                                                    return new Promise(function(resolve) {
                                                        if (window.AndroidBridge) {
                                                            var res = window.AndroidBridge.handleAction('Preferences', 'get', JSON.stringify(options || {}), '');
                                                            try { resolve(JSON.parse(res)); } catch(e) { resolve(res); }
                                                        } else {
                                                            resolve({ value: null });
                                                        }
                                                    });
                                                },
                                                remove: function(options) {
                                                    return new Promise(function(resolve) {
                                                        if (window.AndroidBridge) {
                                                            var res = window.AndroidBridge.handleAction('Preferences', 'remove', JSON.stringify(options || {}), '');
                                                            try { resolve(JSON.parse(res)); } catch(e) { resolve(res); }
                                                        } else {
                                                            resolve({ success: true });
                                                        }
                                                    });
                                                },
                                                clear: function() {
                                                    return new Promise(function(resolve) {
                                                        if (window.AndroidBridge) {
                                                            var res = window.AndroidBridge.handleAction('Preferences', 'clear', '{}', '');
                                                            try { resolve(JSON.parse(res)); } catch(e) { resolve(res); }
                                                        } else {
                                                            resolve({ success: true });
                                                        }
                                                    });
                                                }
                                            };
                                        }
                                        console.log('[NativeBridge] Next.js Capacitor Bridge, Network, Preferences & BiometricAuth active in Android WebView');
                                    })();
                                """.trimIndent()
                                view?.evaluateJavascript(injectShim, null)
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                super.onReceivedError(view, request, error)
                                if (request?.isForMainFrame == true) {
                                    viewModel.addLog("Page load error: ${error?.description} on ${request.url}")
                                }
                            }
                        }

                        loadUrl(uiState.currentUrl)
                    }
                },
                update = { webView ->
                    if (webView.url != uiState.currentUrl && !uiState.isLoading) {
                        webView.loadUrl(uiState.currentUrl)
                    }
                }
            )

            // Linear Progress Indicator
            if (uiState.isLoading) {
                LinearProgressIndicator(
                    progress = { uiState.progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            }
        }
    }

    // Modal Bottom Sheet for Hub, URL Switching & GitHub CI/CD Workflows
    if (uiState.showSettingsSheet) {
        SettingsAndDeploymentSheet(
            uiState = uiState,
            onDismiss = { viewModel.toggleSettingsSheet(false) },
            onSelectEmbedded = {
                viewModel.loadEmbedded()
                webViewRef?.loadUrl("file:///android_asset/www/index.html")
                viewModel.toggleSettingsSheet(false)
            },
            onSelectLocalDev = {
                viewModel.loadLocalDev()
                webViewRef?.loadUrl("http://10.0.2.2:3000")
                viewModel.toggleSettingsSheet(false)
            },
            onApplyCustomUrl = { url ->
                viewModel.setUrl(url)
                webViewRef?.loadUrl(url)
                viewModel.toggleSettingsSheet(false)
            },
            onClearLogs = { viewModel.clearLogs() },
            onTriggerHaptic = { style ->
                bridgeRef?.handleAction("Haptics", "impact", "{\"style\":\"$style\"}", "test_haptic")
            },
            onTriggerToast = { msg ->
                bridgeRef?.handleAction("Toast", "show", "{\"text\":\"$msg\"}", "test_toast")
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAndDeploymentSheet(
    uiState: com.example.viewmodel.AppUiState,
    onDismiss: () -> Unit,
    onSelectEmbedded: () -> Unit,
    onSelectLocalDev: () -> Unit,
    onApplyCustomUrl: (String) -> Unit,
    onClearLogs: () -> Unit,
    onTriggerHaptic: (String) -> Unit,
    onTriggerToast: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) }
    var inputUrl by remember { mutableStateOf(uiState.currentUrl) }
    val clipboard = LocalClipboardManager.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Next.js Capacitor Management Hub",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Configure web environment, GitHub CI/CD, and Bridge APIs",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .padding(bottom = 16.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("URL / Host", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("CI/CD Sync", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Bridge Logs", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            when (selectedTab) {
                0 -> {
                    // TAB 0: URL & Host Config
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Quick Switch Environment",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onSelectEmbedded,
                                    modifier = Modifier.weight(1f).testTag("switch_embedded_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (uiState.isEmbedded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Text("Embedded Bundle", fontSize = 11.sp)
                                }
                                Button(
                                    onClick = onSelectLocalDev,
                                    modifier = Modifier.weight(1f).testTag("switch_localhost_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (uiState.currentUrl.contains("10.0.2.2")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Text("Localhost :3000", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        label = { Text("Custom Next.js App URL (Vercel / Staging / Dev)") },
                        placeholder = { Text("https://your-nextjs-app.vercel.app") },
                        modifier = Modifier.fillMaxWidth().testTag("custom_url_input"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { onApplyCustomUrl(inputUrl) },
                        modifier = Modifier.fillMaxWidth().testTag("apply_url_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Navigate to URL")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Native Hardware Test",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onTriggerHaptic("heavy") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text("Test Haptics", fontSize = 11.sp)
                        }
                        Button(
                            onClick = { onTriggerToast("Native Android Bridge Toast Verified!") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text("Test Toast", fontSize = 11.sp)
                        }
                    }
                }

                1 -> {
                    // TAB 1: CI/CD & GitHub Two-Way Sync
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Text("Automated Deployment on Push", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                    Text(
                                        text = "A ready-to-run GitHub Actions workflow has been created at .github/workflows/deploy-android.yml. When you connect your repository, every git push will automatically build APKs and publish releases.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        item {
                            Text("How to Connect Your GitHub Repo:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            val stepText = """
1. Click the GitHub / Sync button in the top right menu of AI Studio.
2. Select your repository to enable continuous 2-way synchronization.
3. Every commit pushed to GitHub triggers '.github/workflows/deploy-android.yml'.
4. Download signed APKs from GitHub Actions -> Releases!
                            """.trimIndent()
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = stepText,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Next.js Export Command:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                IconButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString("npm run build && npx next export && cp -r out/* app/src/main/assets/www/"))
                                    }
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy command", modifier = Modifier.size(16.dp))
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF020617), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "npm run build && cp -r out/* app/src/main/assets/www/",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: Live Bridge Logs with Color-Coded Log Levels
                    var filterLevel by remember { mutableStateOf<com.example.viewmodel.LogLevel?>(null) }
                    val filteredLogs = remember(uiState.logs, filterLevel) {
                        if (filterLevel == null) uiState.logs
                        else uiState.logs.filter { it.level == filterLevel }
                    }

                    val infoCount = uiState.logs.count { it.level == com.example.viewmodel.LogLevel.INFO || it.level == com.example.viewmodel.LogLevel.WARN }
                    val successCount = uiState.logs.count { it.level == com.example.viewmodel.LogLevel.SUCCESS }
                    val errorCount = uiState.logs.count { it.level == com.example.viewmodel.LogLevel.ERROR }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Live Bridge Event Stream", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        val fullLogText = uiState.logs.joinToString("\n") { "[${it.level.name}] ${it.timestamp} - ${it.message}" }
                                        clipboard.setText(AnnotatedString(fullLogText))
                                        onTriggerToast("Copied ${uiState.logs.size} log entries")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy", fontSize = 11.sp)
                                }
                                Button(
                                    onClick = onClearLogs,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Clear", fontSize = 11.sp)
                                }
                            }
                        }

                        // Filter Pills
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // ALL
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (filterLevel == null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { filterLevel = null }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "ALL (${uiState.logs.size})",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (filterLevel == null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            // INFO
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (filterLevel == com.example.viewmodel.LogLevel.INFO) Color(0xFF381E72) else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { filterLevel = com.example.viewmodel.LogLevel.INFO }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "INFO ($infoCount)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (filterLevel == com.example.viewmodel.LogLevel.INFO) Color(0xFFBAC3FF) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            // SUCCESS
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (filterLevel == com.example.viewmodel.LogLevel.SUCCESS) Color(0xFF064E3B) else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { filterLevel = com.example.viewmodel.LogLevel.SUCCESS }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "SUCCESS ($successCount)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (filterLevel == com.example.viewmodel.LogLevel.SUCCESS) Color(0xFF4ADE80) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            // ERROR
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (filterLevel == com.example.viewmodel.LogLevel.ERROR) Color(0xFF601410) else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { filterLevel = com.example.viewmodel.LogLevel.ERROR }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "ERROR ($errorCount)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (filterLevel == com.example.viewmodel.LogLevel.ERROR) Color(0xFFF2B8B5) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF141316))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (filteredLogs.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No bridge events recorded for this filter.", color = Color.Gray, fontSize = 11.sp)
                                    }
                                }
                            } else {
                                items(filteredLogs, key = { it.id }) { log ->
                                    val (badgeBg, badgeColor, textColor) = when (log.level) {
                                        com.example.viewmodel.LogLevel.INFO -> Triple(Color(0xFF262338), Color(0xFFBAC3FF), Color(0xFFE2E8F0))
                                        com.example.viewmodel.LogLevel.SUCCESS -> Triple(Color(0xFF0F392B), Color(0xFF4ADE80), Color(0xFFDCFCE7))
                                        com.example.viewmodel.LogLevel.ERROR -> Triple(Color(0xFF421518), Color(0xFFF2B8B5), Color(0xFFFFE4E6))
                                        com.example.viewmodel.LogLevel.WARN -> Triple(Color(0xFF3F270B), Color(0xFFFBBF24), Color(0xFFFEF3C7))
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF1C1B1F))
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Level Badge
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(badgeBg)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = log.level.name,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = badgeColor
                                            )
                                        }

                                        // Timestamp
                                        Text(
                                            text = log.timestamp,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.Gray,
                                            modifier = Modifier.padding(top = 1.dp)
                                        )

                                        // Message
                                        Text(
                                            text = log.message,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = textColor,
                                            lineHeight = 15.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
