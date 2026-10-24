package com.kenapps.unplug.monitor

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import com.kenapps.unplug.domain.ReminderSession
import com.kenapps.unplug.data.DiagnosticPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

/** User-enabled monitoring. Never starts from boot or a power receiver. */
class CompatibilityService : Service() {
    private lateinit var alerts: NativeAlerts
    private val session = ReminderSession()
    private val handler = Handler(Looper.getMainLooper())
    private var registered = false
    private var started = false
    private var testMode = false
    private val preferences by lazy { DiagnosticPreferences(applicationContext) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate +
        CoroutineExceptionHandler { _, error ->
            CompatibilityStatus.update(this, CompatibilityStatus.state.value.copy(error = "Settings could not be saved: ${error.message}"))
            stopSelf()
        })
    private val resumeAfterPause = Runnable { reconcile("15-minute pause ended") }
    private val finishTest = Runnable {
        testMode = false
        session.dismiss()
        alerts.removeReminder()
        reconcile("Test finished · next reminder on next unlock")
    }
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            reconcile(intent.action?.substringAfterLast('.') ?: "Power state changed",
                if (intent.action == Intent.ACTION_SCREEN_OFF) false else null)
        }
    }

    override fun onCreate() {
        super.onCreate()
        alerts = NativeAlerts(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            START -> {
                if (!started) {
                    startForeground(NativeAlerts.MONITOR_ID, alerts.monitoring(),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                    started = true
                    CompatibilityStatus.update(this, CompatibilityStatus.state.value.copy(
                        running = true, error = null
                    ))
                    scope.launch {
                        preferences.setMonitoringRequested(true)
                        preferences.settings.collect { settings ->
                            alerts.vibrationEnabled = settings.vibration
                            if (!registered) {
                                registerReceiver(receiver, IntentFilter().apply {
                                    addAction(Intent.ACTION_BATTERY_CHANGED)
                                    addAction(Intent.ACTION_USER_PRESENT)
                                    addAction(Intent.ACTION_SCREEN_OFF)
                                    addAction(Intent.ACTION_SCREEN_ON)
                                }, RECEIVER_NOT_EXPORTED)
                                registered = true
                            }
                            reconcile("Monitoring ready")
                        }
                    }
                }
                if (registered) reconcile("Monitoring resumed")
            }
            STOP -> scope.launch { preferences.setMonitoringRequested(false); stopSelf() }
            else -> {
                if (!started) { stopSelf(); return START_NOT_STICKY }
                when (intent?.action) {
                    DISMISS -> {
                        testMode = false
                        handler.removeCallbacks(finishTest)
                        session.dismiss()
                        alerts.removeReminder()
                        reconcile("Dismissed until the next unlock")
                    }
                    PAUSE -> {
                        testMode = false
                        handler.removeCallbacks(finishTest)
                        session.pause(SystemClock.elapsedRealtime())
                        handler.removeCallbacks(resumeAfterPause)
                        handler.postDelayed(resumeAfterPause, 15 * 60_000L)
                        reconcile("Paused for 15 minutes")
                    }
                    RESUME -> {
                        session.resume()
                        handler.removeCallbacks(resumeAfterPause)
                        reconcile("Resumed")
                    }
                    TEST -> {
                        testMode = true
                        handler.removeCallbacks(finishTest)
                        handler.postDelayed(finishTest, 15_000)
                        session.resetTest()
                        reconcile("Test mode · clears in 15 seconds")
                    }
                    else -> reconcile("State refreshed")
                }
            }
        }
        // After force-stop/process loss, the dashboard offers an explicit restart.
        return START_NOT_STICKY
    }

    private fun reconcile(event: String, unlocked: Boolean? = null) {
        CompatibilityStatus.read(this, event, unlocked)
        val s = CompatibilityStatus.state.value
        if (!s.plugged || !s.unlocked) {
            testMode = false
            handler.removeCallbacks(finishTest)
        }
        if (!s.notificationsAllowed || !alerts.reminderChannelEnabled() || !s.promotionAllowed) {
            CompatibilityStatus.update(this, s.copy(error = "Enable notifications, the reminder channel, and Live Alerts, then restart reminders."))
            stopSelf()
            return
        }
        try {
            when (session.reconcile(s.plugged, s.unlocked, SystemClock.elapsedRealtime())) {
                ReminderSession.Effect.SHOW -> {
                    val eligible = alerts.showReminder(testMode)
                    CompatibilityStatus.update(this, CompatibilityStatus.state.value.copy(structurallyEligible = eligible))
                    // Bounded post-delivery checks, not continuous background polling.
                    handler.postDelayed({ CompatibilityStatus.read(this) }, 500)
                    handler.postDelayed({ CompatibilityStatus.read(this) }, 2000)
                }
                ReminderSession.Effect.REMOVE -> alerts.removeReminder()
                ReminderSession.Effect.NONE -> Unit
            }
            CompatibilityStatus.update(this, CompatibilityStatus.state.value.copy(pausedUntil = session.pausedUntil, testMode = testMode))
            CompatibilityStatus.read(this, unlockedOverride = unlocked)
        } catch (exception: SecurityException) {
            CompatibilityStatus.update(this, CompatibilityStatus.state.value.copy(error = exception.message ?: "Notification permission denied"))
            stopSelf()
        }
    }

    override fun onDestroy() {
        if (registered) unregisterReceiver(receiver)
        scope.cancel()
        handler.removeCallbacksAndMessages(null)
        alerts.removeReminder()
        stopForeground(STOP_FOREGROUND_REMOVE)
        CompatibilityStatus.update(this, CompatibilityStatus.state.value.copy(
            running = false, posted = false, promoted = false, pausedUntil = 0, testMode = false,
            lastEvent = "Monitoring stopped. Enable reminders to resume."
        ))
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val START = "com.kenapps.unplug.START"
        const val STOP = "com.kenapps.unplug.STOP"
        const val DISMISS = "com.kenapps.unplug.DISMISS"
        const val PAUSE = "com.kenapps.unplug.PAUSE"
        const val RESUME = "com.kenapps.unplug.RESUME"
        const val TEST = "com.kenapps.unplug.TEST"
    }
}
