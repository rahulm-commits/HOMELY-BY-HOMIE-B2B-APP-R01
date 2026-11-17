package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class ServerSettings(
  val globalSyncEnabled: Boolean = true,
  val laptopServerEnabled: Boolean = false,
  val laptopServerUrl: String = "http://192.168.1.100:8787",
  val laptopApiToken: String = "",
  val autoDeleteSyncedFromPhone: Boolean = false,
  val lastSyncAtMillis: Long = 0L,
  val lastSyncSummary: String = "Not synced yet",
)

class ServerConfigStore(private val context: Context) {

  private val dataStore: DataStore<Preferences> by lazy {
    getOrCreateDataStore(context.applicationContext)
  }

  val settingsFlow: Flow<ServerSettings> = dataStore.data.map { prefs ->
    ServerSettings(
      globalSyncEnabled = prefs[KEY_GLOBAL_ENABLED] ?: true,
      laptopServerEnabled = prefs[KEY_LAPTOP_ENABLED] ?: false,
      laptopServerUrl = prefs[KEY_LAPTOP_URL] ?: "http://192.168.1.100:8787",
      laptopApiToken = prefs[KEY_LAPTOP_TOKEN] ?: "",
      autoDeleteSyncedFromPhone = prefs[KEY_AUTO_DELETE_SYNCED] ?: false,
      lastSyncAtMillis = prefs[KEY_LAST_SYNC_AT] ?: 0L,
      lastSyncSummary = prefs[KEY_LAST_SYNC_SUMMARY] ?: "Not synced yet",
    )
  }

  suspend fun getCurrentSettings(): ServerSettings = settingsFlow.first()

  suspend fun updateServerConfig(
    globalSyncEnabled: Boolean,
    laptopServerEnabled: Boolean,
    laptopServerUrl: String,
    laptopApiToken: String,
    autoDeleteSyncedFromPhone: Boolean,
  ) {
    val cleanUrl = normalizeServerUrl(laptopServerUrl)
    dataStore.edit { prefs ->
      prefs[KEY_GLOBAL_ENABLED] = globalSyncEnabled
      prefs[KEY_LAPTOP_ENABLED] = laptopServerEnabled
      prefs[KEY_LAPTOP_URL] = cleanUrl
      prefs[KEY_LAPTOP_TOKEN] = laptopApiToken.trim()
      prefs[KEY_AUTO_DELETE_SYNCED] = autoDeleteSyncedFromPhone
    }
  }

  suspend fun recordSyncOutcome(summary: String) {
    dataStore.edit { prefs ->
      prefs[KEY_LAST_SYNC_AT] = System.currentTimeMillis()
      prefs[KEY_LAST_SYNC_SUMMARY] = summary
    }
  }

  companion object {
    private val KEY_GLOBAL_ENABLED = booleanPreferencesKey("global_sync_enabled")
    private val KEY_LAPTOP_ENABLED = booleanPreferencesKey("laptop_server_enabled")
    private val KEY_LAPTOP_URL = stringPreferencesKey("laptop_server_url")
    private val KEY_LAPTOP_TOKEN = stringPreferencesKey("laptop_api_token")
    private val KEY_AUTO_DELETE_SYNCED = booleanPreferencesKey("auto_delete_synced")
    private val KEY_LAST_SYNC_AT = longPreferencesKey("last_sync_at")
    private val KEY_LAST_SYNC_SUMMARY = stringPreferencesKey("last_sync_summary")

    private val dataStores = ConcurrentHashMap<String, DataStore<Preferences>>()

    private fun getOrCreateDataStore(appContext: Context): DataStore<Preferences> {
      val file = appContext.preferencesDataStoreFile("server_settings")
      return dataStores.getOrPut(file.canonicalPath) {
        PreferenceDataStoreFactory.create { file }
      }
    }

    fun normalizeServerUrl(raw: String): String {
      val trimmed = raw.trim().trimEnd('/')
      if (trimmed.isEmpty()) return ""
      return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        trimmed
      } else {
        "http://$trimmed"
      }
    }
  }
}
