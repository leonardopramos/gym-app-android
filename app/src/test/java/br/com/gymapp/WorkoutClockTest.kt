package br.com.gymapp

import br.com.gymapp.feature.WorkoutClock
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutClockTest {
    @Test fun elapsedUsesTimestampAndNeverGoesNegative() {
        assertEquals(32, WorkoutClock.elapsed(1_000L, 33_000L))
        assertEquals(0, WorkoutClock.elapsed(33_000L, 1_000L))
    }
    @Test fun remainingUsesEndTimestampAndExpiresAtZero() {
        assertEquals(27, WorkoutClock.remaining(40_000L, 13_000L))
        assertEquals(0, WorkoutClock.remaining(40_000L, 40_001L))
        assertEquals(0, WorkoutClock.remaining(null, 40_000L))
    }
}
