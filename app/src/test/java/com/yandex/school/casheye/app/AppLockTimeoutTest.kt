package com.yandex.school.casheye.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockTimeoutTest {
    @Test
    fun `locks immediately after the app returns from background`() {
        val backgroundedAtElapsedRealtime = 10_000L

        val shouldLock =
            shouldLockAfterBackground(
                backgroundedAtElapsedRealtime = backgroundedAtElapsedRealtime,
                currentElapsedRealtime = backgroundedAtElapsedRealtime + 1L,
            )

        assertTrue(shouldLock)
    }

    @Test
    fun `does not lock before the preserved five-minute grace period expires`() {
        val backgroundedAtElapsedRealtime = 10_000L

        val shouldLock =
            shouldLockAfterBackground(
                backgroundedAtElapsedRealtime = backgroundedAtElapsedRealtime,
                currentElapsedRealtime =
                    backgroundedAtElapsedRealtime + APP_LOCK_BACKGROUND_GRACE_PERIOD.inWholeMilliseconds - 1L,
                policy = AppLockBackgroundLockPolicy.AFTER_GRACE_PERIOD,
            )

        assertFalse(shouldLock)
    }

    @Test
    fun `locks when the preserved five-minute grace period expires`() {
        val backgroundedAtElapsedRealtime = 10_000L

        val shouldLock =
            shouldLockAfterBackground(
                backgroundedAtElapsedRealtime = backgroundedAtElapsedRealtime,
                currentElapsedRealtime =
                    backgroundedAtElapsedRealtime + APP_LOCK_BACKGROUND_GRACE_PERIOD.inWholeMilliseconds,
                policy = AppLockBackgroundLockPolicy.AFTER_GRACE_PERIOD,
            )

        assertTrue(shouldLock)
    }

    @Test
    fun `does not lock without a background timestamp`() {
        assertFalse(
            shouldLockAfterBackground(
                backgroundedAtElapsedRealtime = null,
                currentElapsedRealtime = 10_000L,
            ),
        )
    }
}
