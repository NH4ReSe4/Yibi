package com.dubiao.yibi

import android.app.Application
import com.dubiao.yibi.data.AppDatabase
import com.dubiao.yibi.data.LedgerRepository
import com.dubiao.yibi.data.UserPreferences
import com.dubiao.yibi.reminder.WeeklyReminderScheduler

class YiBiApplication : Application() {
    val repository: LedgerRepository by lazy {
        LedgerRepository(AppDatabase.create(this))
    }

    val userPreferences: UserPreferences by lazy {
        UserPreferences(this)
    }

    override fun onCreate() {
        super.onCreate()
        WeeklyReminderScheduler.createNotificationChannel(this)
        if (userPreferences.weeklyReminderEnabled.value) {
            WeeklyReminderScheduler.schedule(this)
        }
    }
}
