package com.dubiao.yibi.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateManagerTest {
    @Test
    fun onlyHigherVersionCodeIsAnUpdate() {
        assertTrue(AppUpdateManager.isNewerVersion(2, 1))
        assertFalse(AppUpdateManager.isNewerVersion(1, 1))
        assertFalse(AppUpdateManager.isNewerVersion(1, 2))
    }

    @Test
    fun sha256PrefixIsNormalized() {
        val hash = "a".repeat(64)
        assertEquals(hash, AppUpdateManager.normalizeSha256(" sha256:$hash "))
        assertEquals(hash, AppUpdateManager.normalizeSha256("SHA256:$hash"))
    }
}
