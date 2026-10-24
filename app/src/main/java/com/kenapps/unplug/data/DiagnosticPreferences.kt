package com.kenapps.unplug.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.emptyPreferences
import java.io.IOException

private val Context.diagnosticStore by preferencesDataStore("diagnostic_preferences")

data class DiagnosticNotes(val oxygenVersion: String = "", val observation: String = "Not recorded")

enum class Palette { MIST, LAVENDER, SAND }
enum class ThemeMode { SYSTEM, LIGHT, DARK }
data class AppSettings(
    val palette: Palette = Palette.MIST,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val vibration: Boolean = false,
    val monitoringRequested: Boolean = false,
)

class DiagnosticPreferences(private val context: Context) {
    private val oxygen = stringPreferencesKey("oxygen_version")
    private val observation = stringPreferencesKey("pill_observation")
    private val palette = stringPreferencesKey("palette")
    private val themeMode = stringPreferencesKey("theme_mode")
    private val vibration = booleanPreferencesKey("vibration")
    private val monitoringRequested = booleanPreferencesKey("monitoring_requested")
    private val data = context.diagnosticStore.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }
    val notes = data.map { DiagnosticNotes(it[oxygen] ?: "", it[observation] ?: "Not recorded") }
    val settings = data.map {
        AppSettings(
            palette = Palette.entries.firstOrNull { entry -> entry.name == it[palette] } ?: Palette.MIST,
            themeMode = ThemeMode.entries.firstOrNull { entry -> entry.name == it[themeMode] } ?: ThemeMode.SYSTEM,
            vibration = it[vibration] ?: false,
            monitoringRequested = it[monitoringRequested] ?: false,
        )
    }

    suspend fun saveOxygen(value: String) { context.diagnosticStore.edit { it[oxygen] = value } }
    suspend fun saveObservation(value: String) { context.diagnosticStore.edit { it[observation] = value } }
    suspend fun savePalette(value: Palette) { context.diagnosticStore.edit { it[palette] = value.name } }
    suspend fun saveThemeMode(value: ThemeMode) { context.diagnosticStore.edit { it[themeMode] = value.name } }
    suspend fun saveVibration(value: Boolean) { context.diagnosticStore.edit { it[vibration] = value } }
    suspend fun setMonitoringRequested(value: Boolean) { context.diagnosticStore.edit { it[monitoringRequested] = value } }
}
