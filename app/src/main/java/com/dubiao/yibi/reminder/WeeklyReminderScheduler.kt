package com.dubiao.yibi.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dubiao.yibi.MainActivity
import com.dubiao.yibi.R
import com.dubiao.yibi.data.UserPreferences
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

object WeeklyReminderScheduler {
    fun schedule(context: Context, now: ZonedDateTime = ZonedDateTime.now()) {
        createNotificationChannel(context)
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val triggerAtMillis = nextWeeklyReminder(now).toInstant().toEpochMilli()
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            reminderPendingIntent(context),
        )
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(reminderPendingIntent(context))
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "每周记账提醒",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "每周六上午 10 点提醒整理本周账目"
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    internal fun showNotification(context: Context) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val openAppIntent = PendingIntent.getActivity(
            context,
            OPEN_APP_REQUEST_CODE,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.launcher_background))
            .setContentTitle("本周账目，可以整理啦")
            .setContentText("花一分钟记下本周收支，预算和报表会更准确。")
            .setContentIntent(openAppIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    internal fun reminderPendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REMINDER_REQUEST_CODE,
        Intent(context, WeeklyReminderReceiver::class.java).setAction(ACTION_WEEKLY_REMINDER),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    internal const val NOTIFICATION_ID = 1_001
    private const val CHANNEL_ID = "weekly_ledger_reminder"
    private const val ACTION_WEEKLY_REMINDER = "com.dubiao.yibi.action.WEEKLY_REMINDER"
    private const val REMINDER_REQUEST_CODE = 7_001
    private const val OPEN_APP_REQUEST_CODE = 7_002
}

internal fun nextWeeklyReminder(now: ZonedDateTime): ZonedDateTime {
    val nextSaturday = now.toLocalDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
    var candidate = ZonedDateTime.of(nextSaturday, LocalTime.of(10, 0), now.zone)
    if (!candidate.isAfter(now)) candidate = candidate.plusWeeks(1)
    return candidate
}

class WeeklyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val preferences = UserPreferences(context)
        if (!preferences.weeklyReminderEnabled.value) return
        WeeklyReminderScheduler.createNotificationChannel(context)
        WeeklyReminderScheduler.showNotification(context)
        WeeklyReminderScheduler.schedule(context)
    }
}

class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val enabled = UserPreferences(context).weeklyReminderEnabled.value
        if (enabled) WeeklyReminderScheduler.schedule(context) else WeeklyReminderScheduler.cancel(context)
    }
}
