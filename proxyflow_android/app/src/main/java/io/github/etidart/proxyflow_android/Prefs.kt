package io.github.etidart.proxyflow_android

import android.content.Context
import androidx.core.content.edit

/**
 * The three proxy list sources offered on the main screen.
 */
enum class SourceMode {
    MANUAL,
    FILE,
    URL
}

/**
 * Thin wrapper around SharedPreferences that owns every persisted piece of UI state.
 */
class Prefs(private val appContext: Context) {

    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var sourceMode: SourceMode
        get() = when (prefs.getString(KEY_SOURCE_MODE, null)) {
            SourceMode.MANUAL.name -> SourceMode.MANUAL
            SourceMode.FILE.name -> SourceMode.FILE
            SourceMode.URL.name -> SourceMode.URL
            else -> if (hasProxyList()) SourceMode.FILE else SourceMode.MANUAL
        }
        set(value) = prefs.edit { putString(KEY_SOURCE_MODE, value.name) }

    var manualText: String
        get() = prefs.getString(KEY_MANUAL_TEXT, "").orEmpty()
        set(value) = prefs.edit { putString(KEY_MANUAL_TEXT, value) }

    var lastDownloadUrl: String?
        get() = prefs.getString(KEY_LAST_DOWNLOAD_URL, null)
        set(value) = prefs.edit { putString(KEY_LAST_DOWNLOAD_URL, value) }

    var lastDownloadTimestamp: Long
        get() = prefs.getLong(KEY_LAST_DOWNLOAD_TIMESTAMP, 0L)
        set(value) = prefs.edit { putLong(KEY_LAST_DOWNLOAD_TIMESTAMP, value) }

    private fun hasProxyList(): Boolean =
        appContext.getFileStreamPath(PflowService.PFILENAME).exists()

    companion object {
        private const val PREFS_NAME = "proxyflow"
        private const val KEY_SOURCE_MODE = "source_mode"
        private const val KEY_MANUAL_TEXT = "manual_text"
        private const val KEY_LAST_DOWNLOAD_URL = "last_download_url"
        private const val KEY_LAST_DOWNLOAD_TIMESTAMP = "last_download_timestamp"
    }
}
