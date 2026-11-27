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

import org.json.JSONObject

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

  val packedOrderOverridesFlow: Flow<Map<String, Boolean>> = dataStore.data.map { prefs ->
    decodePackedOverrides(prefs[KEY_PACKED_ORDER_OVERRIDES])
  }

  val stockPendingReasonsFlow: Flow<Map<String, String>> = dataStore.data.map { prefs ->
    decodeStringMap(prefs[KEY_STOCK_PENDING_REASONS])
  }

  suspend fun getCurrentSettings(): ServerSettings = settingsFlow.first()

  suspend fun setOrderPacked(orderId: String, isPacked: Boolean) {
    if (orderId.isBlank()) return
    dataStore.edit { prefs ->
      val current = decodePackedOverrides(prefs[KEY_PACKED_ORDER_OVERRIDES]).toMutableMap()
      current[orderId] = isPacked
      prefs[KEY_PACKED_ORDER_OVERRIDES] = encodePackedOverrides(current)

      val stockPending = decodeStringMap(prefs[KEY_STOCK_PENDING_REASONS]).toMutableMap()
      if (stockPending.remove(orderId) != null) {
        prefs[KEY_STOCK_PENDING_REASONS] = encodeStringMap(stockPending)
      }
    }
  }

  suspend fun setOrdersPacked(orderIds: List<String>, isPacked: Boolean) {
    val validIds = orderIds.filter { it.isNotBlank() }
    if (validIds.isEmpty()) return
    dataStore.edit { prefs ->
      val current = decodePackedOverrides(prefs[KEY_PACKED_ORDER_OVERRIDES]).toMutableMap()
      validIds.forEach { current[it] = isPacked }
      prefs[KEY_PACKED_ORDER_OVERRIDES] = encodePackedOverrides(current)

      val stockPending = decodeStringMap(prefs[KEY_STOCK_PENDING_REASONS]).toMutableMap()
      var changed = false
      validIds.forEach { id ->
        if (stockPending.remove(id) != null) changed = true
      }
      if (changed) {
        prefs[KEY_STOCK_PENDING_REASONS] = encodeStringMap(stockPending)
      }
    }
  }

  suspend fun markOrderStockPending(orderId: String, reason: String) {
    if (orderId.isBlank()) return
    val cleanReason = reason.trim().ifBlank { "Stock unavailable" }.take(180)
    dataStore.edit { prefs ->
      val packed = decodePackedOverrides(prefs[KEY_PACKED_ORDER_OVERRIDES]).toMutableMap()
      packed[orderId] = false
      prefs[KEY_PACKED_ORDER_OVERRIDES] = encodePackedOverrides(packed)

      val stockPending = decodeStringMap(prefs[KEY_STOCK_PENDING_REASONS]).toMutableMap()
      stockPending[orderId] = cleanReason
      prefs[KEY_STOCK_PENDING_REASONS] = encodeStringMap(stockPending)
    }
  }

  suspend fun clearOrderStockPending(orderId: String) {
    if (orderId.isBlank()) return
    dataStore.edit { prefs ->
      val stockPending = decodeStringMap(prefs[KEY_STOCK_PENDING_REASONS]).toMutableMap()
      if (stockPending.remove(orderId) != null) {
        prefs[KEY_STOCK_PENDING_REASONS] = encodeStringMap(stockPending)
      }
    }
  }

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
    private val KEY_PACKED_ORDER_OVERRIDES = stringPreferencesKey("packed_order_overrides")
    private val KEY_STOCK_PENDING_REASONS = stringPreferencesKey("stock_pending_reasons")

    private val dataStores = ConcurrentHashMap<String, DataStore<Preferences>>()

    private fun getOrCreateDataStore(appContext: Context): DataStore<Preferences> {
      val file = appContext.preferencesDataStoreFile("server_settings")
      return dataStores.getOrPut(file.canonicalPath) {
        PreferenceDataStoreFactory.create { file }
      }
    }

    private fun decodePackedOverrides(raw: String?): Map<String, Boolean> {
      if (raw.isNullOrBlank()) return emptyMap()
      return runCatching {
        val json = JSONObject(raw)
        buildMap {
          val keys = json.keys()
          while (keys.hasNext()) {
            val key = keys.next()
            put(key, json.optBoolean(key, false))
          }
        }
      }.getOrDefault(emptyMap())
    }

    private fun encodePackedOverrides(map: Map<String, Boolean>): String {
      val json = JSONObject()
      map.forEach { (k, v) -> json.put(k, v) }
      return json.toString()
    }

    private fun decodeStringMap(raw: String?): Map<String, String> {
      if (raw.isNullOrBlank()) return emptyMap()
      return runCatching {
        val json = JSONObject(raw)
        buildMap {
          val keys = json.keys()
          while (keys.hasNext()) {
            val key = keys.next()
            val value = json.optString(key, "").trim()
            if (value.isNotEmpty()) put(key, value)
          }
        }
      }.getOrDefault(emptyMap())
    }

    private fun encodeStringMap(map: Map<String, String>): String {
      val json = JSONObject()
      map.forEach { (k, v) -> json.put(k, v) }
      return json.toString()
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
