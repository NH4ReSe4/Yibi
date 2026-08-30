package com.dubiao.yibi.reminder

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dubiao.yibi.data.UserPreferences
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WeeklyReminderReceiverTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val notificationManager by lazy { context.getSystemService(NotificationManager::class.java) }

    @Before
    fun prepare() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(
                context.packageName,
                Manifest.permission.POST_NOTIFICATIONS,
            )
        }
        UserPreferences(context).setWeeklyReminderEnabled(true)
        notificationManager.cancel(WeeklyReminderScheduler.NOTIFICATION_ID)
    }

    @After
    fun cleanUp() {
        notificationManager.cancel(WeeklyReminderScheduler.NOTIFICATION_ID)
        WeeklyReminderScheduler.cancel(context)
    }

    @Test
    fun receiverPostsTheWeeklyReminderNotification() {
        WeeklyReminderReceiver().onReceive(context, Intent())
        SystemClock.sleep(200)

        val notification = notificationManager.activeNotifications
            .firstOrNull { it.id == WeeklyReminderScheduler.NOTIFICATION_ID }
            ?.notification
        assertTrue("每周提醒通知未出现", notification != null)
        assertEquals("本周账目，可以整理啦", notification?.extras?.getCharSequence(Notification.EXTRA_TITLE))
    }
}
