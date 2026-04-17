package com.example.myapplication.data.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules / cancels the daily study reminder notification.
 * Uses AlarmManager with setExactAndAllowWhileIdle for precise timing,
 * even when the device is in Doze mode.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private val prefs by lazy {
        context.getSharedPreferences("study_settings", Context.MODE_PRIVATE)
    }

    companion object {
        private const val TAG = "ReminderScheduler"
        private const val REQUEST_CODE = 2001
    }

    /**
     * Schedule a daily reminder at [hour]:[minute].
     * Replaces any existing reminder.
     */
    fun schedule(hour: Int = 20, minute: Int = 0) {
        prefs.edit()
            .putBoolean("reminder_enabled", true)
            .putInt("reminder_hour", hour)
            .putInt("reminder_minute", minute)
            .apply()

        val triggerTime = computeNextTriggerTime(hour, minute)
        val pendingIntent = createPendingIntent()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    // Fallback: inexact alarm (still better than WorkManager for timing)
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            Log.d(TAG, "Reminder scheduled for $hour:$minute (trigger in ${(triggerTime - System.currentTimeMillis()) / 60000} min)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm: ${e.message}")
            // Fallback to inexact
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    /**
     * Cancel the daily reminder.
     */
    fun cancel() {
        prefs.edit().putBoolean("reminder_enabled", false).apply()
        alarmManager.cancel(createPendingIntent())
        Log.d(TAG, "Reminder cancelled")
    }

    fun isEnabled(): Boolean = prefs.getBoolean("reminder_enabled", false)

    fun getHour(): Int = prefs.getInt("reminder_hour", 20)

    fun getMinute(): Int = prefs.getInt("reminder_minute", 0)

    /**
     * Calculate the next trigger time for [hour]:[minute].
     * If the time already passed today, schedule for tomorrow.
     */
    private fun computeNextTriggerTime(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // If target time already passed today, schedule for tomorrow
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis
    }

    private fun createPendingIntent(): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
