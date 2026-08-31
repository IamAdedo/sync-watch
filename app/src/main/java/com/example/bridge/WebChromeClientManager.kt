package com.example.bridge

import android.net.Uri
import android.webkit.ConsoleMessage
import android.webkit.GeolocationPermissions
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView

class WebChromeClientManager(
    private val onProgressUpdate: (Int) -> Unit = {},
    private val onConsoleLog: (String) -> Unit = {},
    private val onFileChooser: (ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Boolean = { _, _ -> false }
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        onProgressUpdate(newProgress)
    }

    override fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        callback: GeolocationPermissions.Callback?
    ) {
        // Automatically grant geolocation permission to WebView origin for hybrid wrapper
        callback?.invoke(origin, true, false)
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        if (consoleMessage != null) {
            val tag = when (consoleMessage.messageLevel()) {
                ConsoleMessage.MessageLevel.ERROR -> "[ERROR]"
                ConsoleMessage.MessageLevel.WARNING -> "[WARN]"
                else -> "[LOG]"
            }
            onConsoleLog("$tag ${consoleMessage.message()} (line ${consoleMessage.lineNumber()})")
        }
        return super.onConsoleMessage(consoleMessage)
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        return onFileChooser(filePathCallback, fileChooserParams)
    }
}
