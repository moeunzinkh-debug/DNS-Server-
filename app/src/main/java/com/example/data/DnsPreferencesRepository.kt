package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.DnsProvider
import com.example.model.LanguageMode
import com.example.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dnsPreferencesDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "dns_server_prefs")

data class DnsPreferences(
  val selectedProviderId: String = "google",
  val themeMode: ThemeMode = ThemeMode.DARK,
  val languageMode: LanguageMode = LanguageMode.BILINGUAL,
  val strictDoTOnly: Boolean = false,
  val customProviders: List<DnsProvider> = emptyList(),
)

class DnsPreferencesRepository(private val context: Context) {

  private object Keys {
    val SELECTED_PROVIDER_ID = stringPreferencesKey("selected_provider_id")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val LANGUAGE_MODE = stringPreferencesKey("language_mode")
    val STRICT_DOT_ONLY = booleanPreferencesKey("strict_dot_only")
    val CUSTOM_PROVIDERS_JSON = stringPreferencesKey("custom_providers_json")
  }

  val preferencesFlow: Flow<DnsPreferences> =
    context.dnsPreferencesDataStore.data.map { prefs ->
      val theme =
        runCatching { ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.DARK.name) }
          .getOrDefault(ThemeMode.DARK)
      val language =
        runCatching { LanguageMode.valueOf(prefs[Keys.LANGUAGE_MODE] ?: LanguageMode.BILINGUAL.name) }
          .getOrDefault(LanguageMode.BILINGUAL)
      val custom = decodeCustomProviders(prefs[Keys.CUSTOM_PROVIDERS_JSON])
      DnsPreferences(
        selectedProviderId = prefs[Keys.SELECTED_PROVIDER_ID] ?: "google",
        themeMode = theme,
        languageMode = language,
        strictDoTOnly = prefs[Keys.STRICT_DOT_ONLY] ?: false,
        customProviders = custom,
      )
    }

  suspend fun setSelectedProviderId(providerId: String) {
    context.dnsPreferencesDataStore.edit { prefs -> prefs[Keys.SELECTED_PROVIDER_ID] = providerId }
  }

  suspend fun setThemeMode(mode: ThemeMode) {
    context.dnsPreferencesDataStore.edit { prefs -> prefs[Keys.THEME_MODE] = mode.name }
  }

  suspend fun setLanguageMode(mode: LanguageMode) {
    context.dnsPreferencesDataStore.edit { prefs -> prefs[Keys.LANGUAGE_MODE] = mode.name }
  }

  suspend fun setStrictDoTOnly(strict: Boolean) {
    context.dnsPreferencesDataStore.edit { prefs -> prefs[Keys.STRICT_DOT_ONLY] = strict }
  }

  suspend fun addCustomProvider(provider: DnsProvider) {
    context.dnsPreferencesDataStore.edit { prefs ->
      val current = decodeCustomProviders(prefs[Keys.CUSTOM_PROVIDERS_JSON]).toMutableList()
      current.removeAll { it.id == provider.id }
      current.add(provider)
      prefs[Keys.CUSTOM_PROVIDERS_JSON] = encodeCustomProviders(current)
      prefs[Keys.SELECTED_PROVIDER_ID] = provider.id
    }
  }

  suspend fun deleteCustomProvider(providerId: String) {
    context.dnsPreferencesDataStore.edit { prefs ->
      val current = decodeCustomProviders(prefs[Keys.CUSTOM_PROVIDERS_JSON]).toMutableList()
      current.removeAll { it.id == providerId }
      prefs[Keys.CUSTOM_PROVIDERS_JSON] = encodeCustomProviders(current)
      if (prefs[Keys.SELECTED_PROVIDER_ID] == providerId) {
        prefs[Keys.SELECTED_PROVIDER_ID] = "google"
      }
    }
  }

  private fun encodeCustomProviders(list: List<DnsProvider>): String {
    val array = JSONArray()
    list.forEach { provider ->
      val obj =
        JSONObject().apply {
          put("id", provider.id)
          put("name", provider.name)
          put("hostname", provider.hostname)
          put("primaryIpv4", provider.primaryIpv4)
          put("secondaryIpv4", provider.secondaryIpv4)
        }
      array.put(obj)
    }
    return array.toString()
  }

  private fun decodeCustomProviders(json: String?): List<DnsProvider> {
    if (json.isNullOrBlank()) return emptyList()
    return runCatching {
        val array = JSONArray(json)
        buildList {
          for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            add(
              DnsProvider(
                id = obj.getString("id"),
                name = obj.getString("name"),
                hostname = obj.getString("hostname"),
                primaryIpv4 = obj.getString("primaryIpv4"),
                secondaryIpv4 = obj.optString("secondaryIpv4", ""),
                taglineKhmer = "ម៉ាស៊ីនមេ DNS ផ្ទាល់ខ្លួន",
                taglineEnglish = "Custom DNS-over-TLS resolver",
                features = listOf("Custom", "DNS-over-TLS"),
                accentColorHex = 0xFF0EA5E9,
                isCustom = true,
              )
            )
          }
        }
      }
      .getOrDefault(emptyList())
  }
}
