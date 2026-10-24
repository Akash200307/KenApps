package com.kenapps.unplug.monitor

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File

data class TestStatus(
    val running: Boolean = false,
    val plugged: Boolean = false,
    val batteryPercent: Int? = null,
    val unlocked: Boolean = false,
    val notificationsAllowed: Boolean = false,
    val promotionAllowed: Boolean = false,
    val structurallyEligible: Boolean? = null,
    val posted: Boolean = false,
    val promoted: Boolean = false,
    val pausedUntil: Long = 0,
    val error: String? = null,
    val lastEvent: String = "No compatibility session started",
    val oxygenVersion: String = "Not recorded",
    val observation: String = "Not recorded",
    val testMode: Boolean = false,
    val palette: String = "MIST",
    val themeMode: String = "SYSTEM",
    val vibration: Boolean = false,
)

/** Contains only this application's status, never other apps' notifications. */
object CompatibilityStatus {
    private val mutable = MutableStateFlow(TestStatus())
    val state = mutable.asStateFlow()

    fun read(context: Context, event: String? = null, unlockedOverride: Boolean? = null) {
        val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val manager = context.getSystemService(NotificationManager::class.java)
        val notification = manager.activeNotifications.firstOrNull { it.id == NativeAlerts.REMINDER_ID }?.notification
        val now = mutable.value
        update(context, now.copy(
            plugged = (battery?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0) != 0,
            batteryPercent = if (level >= 0 && scale > 0) level * 100 / scale else null,
            unlocked = unlockedOverride ?: (
                context.getSystemService(PowerManager::class.java).isInteractive &&
                    !context.getSystemService(KeyguardManager::class.java).isKeyguardLocked
                ),
            notificationsAllowed = manager.areNotificationsEnabled(),
            promotionAllowed = manager.canPostPromotedNotifications(),
            posted = notification != null,
            promoted = notification?.let { it.flags and Notification.FLAG_PROMOTED_ONGOING != 0 } ?: false,
            lastEvent = event ?: now.lastEvent,
        ))
    }

    fun update(context: Context, value: TestStatus) {
        mutable.value = value
        // Private app storage; diagnostics exclude serial numbers and identifiers.
        File(context.filesDir, "compatibility-report.json").writeText(json(value).toString(2))
    }

    fun json(s: TestStatus = mutable.value): JSONObject = JSONObject().apply {
        put("appVersion", "0.2.0-personal")
        put("manufacturer", Build.MANUFACTURER)
        put("model", Build.MODEL)
        put("androidVersion", Build.VERSION.RELEASE)
        put("api", Build.VERSION.SDK_INT)
        put("buildDisplay", Build.DISPLAY)
        put("securityPatch", Build.VERSION.SECURITY_PATCH)
        put("recordedAtEpochMillis", System.currentTimeMillis())
        put("monitoring", s.running)
        put("plugged", s.plugged)
        put("batteryPercent", s.batteryPercent ?: JSONObject.NULL)
        put("unlocked", s.unlocked)
        put("notificationsAllowed", s.notificationsAllowed)
        put("promotionAllowed", s.promotionAllowed)
        put("structurallyEligible", s.structurallyEligible ?: JSONObject.NULL)
        put("reminderPosted", s.posted)
        put("promotedFlag", s.promoted)
        put("pausedUntilElapsedRealtime", s.pausedUntil)
        put("lastEvent", s.lastEvent)
        put("error", s.error ?: JSONObject.NULL)
        put("testMode", s.testMode)
        put("palette", s.palette)
        put("themeMode", s.themeMode)
        put("vibration", s.vibration)
        put("nativePillReportedByUser", s.observation == "Native pill visible")
        put("usageGuidance", "Ordinary alerts are outside Android Live Updates usage guidance")
        put("distribution", "Personal experimental APK; user accepted the usage-guidance limitation")
        put("oxygenVersionEnteredByUser", s.oxygenVersion)
        put("observationEnteredByUser", s.observation)
    }
}
