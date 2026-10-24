package com.kenapps.unplug.domain

import com.kenapps.unplug.domain.ReminderSession.Effect.*
import org.junit.Assert.*
import org.junit.Test

class ReminderSessionTest {
    @Test fun unpluggedUnlockDoesNothing() {
        assertEquals(NONE, ReminderSession().reconcile(false, true, 0))
    }
    @Test fun lockedChargingDoesNothing() {
        assertEquals(NONE, ReminderSession().reconcile(true, false, 0))
    }
    @Test fun unlockShowsOnlyOnce() {
        val s = ReminderSession()
        s.reconcile(true, false, 0)
        assertEquals(SHOW, s.reconcile(true, true, 1))
        assertEquals(NONE, s.reconcile(true, true, 2))
    }
    @Test fun connectingDuringUseShowsAndDisconnectRemoves() {
        val s = ReminderSession()
        s.reconcile(false, true, 0)
        assertEquals(SHOW, s.reconcile(true, true, 1))
        assertEquals(REMOVE, s.reconcile(false, true, 2))
    }
    @Test fun dismissalSurvivesPowerEventsUntilNextUnlock() {
        val s = ReminderSession()
        s.reconcile(true, true, 0)
        s.dismiss()
        assertEquals(NONE, s.reconcile(true, true, 1))
        assertEquals(NONE, s.reconcile(false, true, 2))
        assertEquals(NONE, s.reconcile(true, true, 3))
        s.reconcile(true, false, 4)
        assertEquals(SHOW, s.reconcile(true, true, 5))
    }
    @Test fun lockingRemovesAndNextUnlockShows() {
        val s = ReminderSession()
        s.reconcile(true, true, 0)
        assertEquals(REMOVE, s.reconcile(true, false, 1))
        assertEquals(SHOW, s.reconcile(true, true, 2))
    }
    @Test fun pauseRemovesAndResumesAtDeadline() {
        val s = ReminderSession()
        s.reconcile(true, true, 0)
        s.pause(1)
        assertEquals(REMOVE, s.reconcile(true, true, 2))
        assertEquals(NONE, s.reconcile(true, true, 900_000))
        assertEquals(SHOW, s.reconcile(true, true, 900_001))
        assertEquals(0L, s.pausedUntil)
    }
    @Test fun pauseSurvivesLockingAndResumeIsExplicit() {
        val s = ReminderSession()
        s.pause(0)
        s.reconcile(true, false, 1)
        assertEquals(NONE, s.reconcile(true, true, 2))
        s.resume()
        assertEquals(SHOW, s.reconcile(true, true, 3))
    }
    @Test fun explicitTestCanResetDismissal() {
        val s = ReminderSession()
        s.reconcile(true, true, 0)
        s.dismiss()
        s.resetTest()
        assertEquals(SHOW, s.reconcile(true, true, 1))
    }
    @Test fun explicitTestCanReplaceAnExistingReminder() {
        val s = ReminderSession()
        assertEquals(SHOW, s.reconcile(true, true, 0))
        s.resetTest()
        assertEquals(SHOW, s.reconcile(true, true, 1))
    }
    @Test fun rapidConnectionChangesNeverDuplicate() {
        val s = ReminderSession()
        repeat(20) {
            assertEquals(SHOW, s.reconcile(true, true, it.toLong()))
            assertEquals(NONE, s.reconcile(true, true, it.toLong()))
            assertEquals(REMOVE, s.reconcile(false, true, it.toLong()))
        }
    }
}
