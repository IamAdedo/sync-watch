package com.example.viewmodel

import androidx.lifecycle.ViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class LogLevel {
    INFO,
    SUCCESS,
    ERROR,
    WARN
}

data class BridgeLog(
    val id: String = UUID.randomUUID().toString(),
    val message: String,
    val level: LogLevel = LogLevel.INFO,
    val timestamp: String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
)

data class AppUiState(
    val currentUrl: String = "file:///android_asset/www/index.html",
    val isEmbedded: Boolean = true,
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val title: String = "Next.js Capacitor App",
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isConnected: Boolean = true,
    val logs: List<BridgeLog> = listOf(
        BridgeLog(message = "Capacitor Bridge initialized & ready", level = LogLevel.SUCCESS)
    ),
    val selectedLogLevelFilter: LogLevel? = null,
    val showSettingsSheet: Boolean = false,
    val showCiCdGuide: Boolean = false,
    val customUrlInput: String = "http://10.0.2.2:3000"
) {
    // Legacy helper property
    val bridgeLogs: List<String>
        get() = logs.map { "[${it.level.name}] ${it.timestamp} - ${it.message}" }
}

class AppViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    fun setUrl(url: String) {
        val cleanUrl = url.trim()
        val isLocalAsset = cleanUrl.startsWith("file://")
        _uiState.update {
            it.copy(
                currentUrl = cleanUrl,
                isEmbedded = isLocalAsset,
                isLoading = true,
                progress = 0
            )
        }
        addLog("Navigating to: $cleanUrl", LogLevel.INFO)
    }

    fun setCustomUrlInput(url: String) {
        _uiState.update { it.copy(customUrlInput = url) }
    }

    fun loadEmbedded() {
        setUrl("file:///android_asset/www/index.html")
    }

    fun loadLocalDev() {
        setUrl("http://10.0.2.2:3000")
    }

    fun setProgress(progress: Int) {
        _uiState.update {
            it.copy(
                progress = progress,
                isLoading = progress < 100
            )
        }
    }

    fun setNavState(canGoBack: Boolean, canGoForward: Boolean, title: String? = null) {
        _uiState.update {
            it.copy(
                canGoBack = canGoBack,
                canGoForward = canGoForward,
                title = title ?: it.title
            )
        }
    }

    fun addLog(log: String, level: LogLevel? = null) {
        val inferredLevel = level ?: when {
            log.contains("error", ignoreCase = true) || log.contains("failed", ignoreCase = true) || log.contains("exception", ignoreCase = true) -> LogLevel.ERROR
            log.contains("success", ignoreCase = true) || log.contains("connected", ignoreCase = true) || log.contains("loaded", ignoreCase = true) || log.contains("saved", ignoreCase = true) -> LogLevel.SUCCESS
            log.contains("warn", ignoreCase = true) || log.contains("caution", ignoreCase = true) -> LogLevel.WARN
            else -> LogLevel.INFO
        }
        val newLog = BridgeLog(message = log, level = inferredLevel)
        _uiState.update {
            val updated = (listOf(newLog) + it.logs).take(100)
            it.copy(logs = updated)
        }
    }

    fun setLogLevelFilter(filter: LogLevel?) {
        _uiState.update { it.copy(selectedLogLevelFilter = filter) }
    }

    fun clearLogs() {
        _uiState.update { it.copy(logs = emptyList()) }
    }

    fun toggleSettingsSheet(show: Boolean) {
        _uiState.update { it.copy(showSettingsSheet = show) }
    }

    fun toggleCiCdGuide(show: Boolean) {
        _uiState.update { it.copy(showCiCdGuide = show) }
    }
}

