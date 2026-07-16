package com.alisu.alauncher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.store: DataStore<Preferences> by preferencesDataStore(name = "alauncher_prefs")

class AppPreferences(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    private object Keys {
        val THEME_ID = stringPreferencesKey("theme_id")
        val ICON_PACK = stringPreferencesKey("icon_pack_package")
        val ICON_SIZE = stringPreferencesKey("icon_size")
        val ICON_SHAPE = stringPreferencesKey("icon_shape")
        val DRAWER_STYLE = stringPreferencesKey("drawer_style")
        val CLOCK_STYLE = stringPreferencesKey("clock_style")
        val SHOW_APP_NAME = booleanPreferencesKey("show_app_name")
        val SHOW_MASCOT = booleanPreferencesKey("show_mascot")
        val MONOCHROMATIC = booleanPreferencesKey("monochromatic_icons")
        val MEDIA_COMPACT = booleanPreferencesKey("media_compact")
        val WEATHER_LOCATION = stringPreferencesKey("weather_location")
        val HOME_APPS = stringPreferencesKey("home_apps_packages")
        val DOCK_APPS = stringPreferencesKey("dock_apps_packages")
        val WIDGET_IDS = stringPreferencesKey("saved_widget_ids")
        val CARD_RADIUS = intPreferencesKey("card_corner_radius_override")
        val ICON_SHAPE_MODE = stringPreferencesKey("icon_shape_mode")
    }

    // ── Reactive flows ──
    val themeId: Flow<String> = flow(Keys.THEME_ID, "base")
    val iconPack: Flow<String> = flow(Keys.ICON_PACK, "none")
    val iconSize: Flow<String> = flow(Keys.ICON_SIZE, "medium")
    val iconShape: Flow<String> = flow(Keys.ICON_SHAPE, "theme")
    val drawerStyle: Flow<String> = flow(Keys.DRAWER_STYLE, "standard")
    val clockStyle: Flow<String> = flow(Keys.CLOCK_STYLE, "default")
    val showAppName: Flow<Boolean> = flow(Keys.SHOW_APP_NAME, true)
    val showMascot: Flow<Boolean> = flow(Keys.SHOW_MASCOT, false)
    val monochromatic: Flow<Boolean> = flow(Keys.MONOCHROMATIC, true)
    val mediaCompact: Flow<Boolean> = flow(Keys.MEDIA_COMPACT, false)
    val cardRadius: Flow<Int> = flow(Keys.CARD_RADIUS, -1)
    val iconShapeMode: Flow<String> = flow(Keys.ICON_SHAPE_MODE, "force_launcher")

    val homeApps: Flow<List<String>> = listFlow(Keys.HOME_APPS)
    val dockApps: Flow<List<String>> = listFlow(Keys.DOCK_APPS)
    val widgetIds: Flow<List<String>> = listFlow(Keys.WIDGET_IDS)

    // ── Sync reads (for existing sync code) ──
    suspend fun getString(key: String): String {
        val k = stringPreferencesKey(key)
        return context.store.data.first()[k] ?: ""
    }
    suspend fun getBoolean(key: String): Boolean {
        val k = booleanPreferencesKey(key)
        return context.store.data.first()[k] ?: false
    }
    suspend fun getInt(key: String): Int {
        val k = intPreferencesKey(key)
        return context.store.data.first()[k] ?: 0
    }

    // ── Async reads (one-shot) ──
    suspend fun getHomeAppsSync(): List<String> = decodeList(context.store.data.first()[Keys.HOME_APPS])
    suspend fun getDockAppsSync(): List<String> = decodeList(context.store.data.first()[Keys.DOCK_APPS])
    suspend fun getWidgetIdsSync(): List<String> = decodeList(context.store.data.first()[Keys.WIDGET_IDS])

    // ── Writes ──
    suspend fun putString(key: String, value: String) {
        context.store.edit { it[stringPreferencesKey(key)] = value }
    }
    suspend fun putBoolean(key: String, value: Boolean) {
        context.store.edit { it[booleanPreferencesKey(key)] = value }
    }
    suspend fun putInt(key: String, value: Int) {
        context.store.edit { it[intPreferencesKey(key)] = value }
    }
    suspend fun remove(key: String) {
        context.store.edit { it.remove(stringPreferencesKey(key)) }
    }
    suspend fun setHomeApps(value: List<String>) {
        context.store.edit { it[Keys.HOME_APPS] = json.encodeToString(value) }
    }
    suspend fun setDockApps(value: List<String>) {
        context.store.edit { it[Keys.DOCK_APPS] = json.encodeToString(value) }
    }
    suspend fun setWidgetIds(value: List<String>) {
        context.store.edit { it[Keys.WIDGET_IDS] = json.encodeToString(value) }
    }

    // ── Internal ──
    private fun <T> flow(key: Preferences.Key<T>, default: T): Flow<T> =
        context.store.data.map { it[key] ?: default }

    private fun listFlow(key: Preferences.Key<String>): Flow<List<String>> =
        context.store.data.map { decodeList(it[key]) }

    private fun decodeList(raw: String?): List<String> {
        if (raw.isNullOrEmpty()) return emptyList()
        return try { json.decodeFromString<List<String>>(raw) }
        catch (_: Exception) { raw.split(",").filter { it.isNotEmpty() } }
    }

    /** Returns all stored keys as a Map (replaces SharedPreferences.getAll()) */
    suspend fun getAll(): Map<String, *> {
        val prefs = context.store.data.first()
        return prefs.asMap().mapKeys { it.key.name }
    }
}
