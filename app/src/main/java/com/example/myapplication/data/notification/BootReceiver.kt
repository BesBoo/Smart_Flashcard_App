package com.example.myapplication.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Re-schedules the daily reminder alarm after device reboot.
 * AlarmManager alarms are cleared on reboot, so we need to restore them.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("study_settings", Context.MODE_PRIVATE)
            if (prefs.getBoolean("reminder_enabled", false)) {
                val hour = prefs.getInt("reminder_hour", 20)
                val minute = prefs.getInt("reminder_minute", 0)
                Log.d("BootReceiver", "Rescheduling reminder for $hour:$minute after reboot")
                ReminderScheduler(context).schedule(hour, minute)
            }
        }
    }
}
