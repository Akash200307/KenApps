package com.kenapps.unplug.domain

/** Pure, event-driven session state. Android supplies power and unlocked state. */
class ReminderSession {
    var visible = false
        private set
    var dismissed = false
        private set
    var pausedUntil = 0L
        private set
    private var unlocked = false

    enum class Effect { SHOW, REMOVE, NONE }

    fun reconcile(plugged: Boolean, nowUnlocked: Boolean, now: Long): Effect {
        if (pausedUntil > 0 && now >= pausedUntil) pausedUntil = 0
        // Locking ends the session; plugging/unplugging alone does not reset dismissal.
        if (!nowUnlocked || !unlocked) dismissed = false
        unlocked = nowUnlocked
        val wanted = plugged && nowUnlocked && !dismissed && now >= pausedUntil
        return when {
            wanted && !visible -> { visible = true; Effect.SHOW }
            !wanted && visible -> { visible = false; Effect.REMOVE }
            else -> Effect.NONE
        }
    }

    fun dismiss() { dismissed = true; visible = false }
    fun pause(now: Long) { pausedUntil = now + 15 * 60_000L }
    fun resume() { pausedUntil = 0L }
    fun resetTest() { dismissed = false; visible = false }
}
