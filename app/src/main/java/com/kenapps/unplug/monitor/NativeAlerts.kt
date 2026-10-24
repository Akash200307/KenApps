package com.kenapps.unplug.monitor

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.kenapps.unplug.MainActivity
import com.kenapps.unplug.R
import androidx.core.app.NotificationCompat

class NativeAlerts(private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java)
    var vibrationEnabled = false

    init {
        manager.createNotificationChannel(NotificationChannel(
            MONITOR_CHANNEL, "Monitoring status", NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Visible while charging reminders are enabled" })
        manager.createNotificationChannel(NotificationChannel(
            REMINDER_CHANNEL, "Unplug reminders · quiet", NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Personal native unplug reminder without sound or vibration"
            setSound(null, null)
            enableVibration(false)
        })
        manager.createNotificationChannel(NotificationChannel(
            VIBRATION_CHANNEL, "Unplug reminders · vibration", NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Personal native unplug reminder with a gentle vibration"
            setSound(null, null)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 60)
        })
    }

    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        context, 0, Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun command(action: String, request: Int): PendingIntent = PendingIntent.getService(
        context, request, Intent(context, CompatibilityService::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun monitoring(): Notification = Notification.Builder(context, MONITOR_CHANNEL)
        .setSmallIcon(R.drawable.ic_unplug)
        .setContentTitle("KenApps · reminders enabled")
        .setContentText("Watching charging and unlock events. Tap to open.")
        .setContentIntent(openApp())
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .addAction(Notification.Action.Builder(null, "Turn off", command(CompatibilityService.STOP, 1)).build())
        .build()

    fun showReminder(testMode: Boolean = false): Boolean {
        if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("Allow notifications before starting a compatibility session")
        }
        // Honest STATUS notification: no fabricated call, media, navigation or progress.
        val notification = NotificationCompat.Builder(context, if (vibrationEnabled) VIBRATION_CHANNEL else REMINDER_CHANNEL)
            .setSmallIcon(R.drawable.ic_unplug)
            .setContentTitle(if (testMode) "Test · Unplug to use your phone" else "Unplug to use your phone")
            .setContentText(if (testMode) "Test reminder · clears in 15 seconds" else "Connected to power · a little pause before you scroll")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                if (testMode) "Test reminder. This preview clears in 15 seconds, or when you lock or unplug."
                else "Unplug to use your phone. Pause reminders for 15 minutes if you need to stay connected."
            ))
            .setCategory(Notification.CATEGORY_STATUS)
            .setContentIntent(openApp())
            .setDeleteIntent(command(CompatibilityService.DISMISS, 2))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setRequestPromotedOngoing(true)
            .setShortCriticalText(if (testMode) "Test" else "Unplug")
            .addAction(0, "Pause 15 min", command(CompatibilityService.PAUSE, 3))
            .addAction(0, "Dismiss", command(CompatibilityService.DISMISS, 2))
            .build()
        manager.notify(REMINDER_ID, notification)
        return notification.hasPromotableCharacteristics()
    }

    fun removeReminder() = manager.cancel(REMINDER_ID)

    fun reminderChannelEnabled(): Boolean =
        manager.getNotificationChannel(if (vibrationEnabled) VIBRATION_CHANNEL else REMINDER_CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE

    companion object {
        const val MONITOR_ID = 100
        const val REMINDER_ID = 101
        const val MONITOR_CHANNEL = "compatibility-monitor"
        const val REMINDER_CHANNEL = "unplug-native-test"
        const val VIBRATION_CHANNEL = "unplug-vibration"
    }
}
