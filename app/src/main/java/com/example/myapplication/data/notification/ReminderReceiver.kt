package com.example.myapplication.data.notification

import android.Manifest
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
import com.example.myapplication.MainActivity
import com.example.myapplication.R

/**
 * BroadcastReceiver that fires the daily study reminder notification.
 * Triggered by AlarmManager at the exact time set by the user.
 * After showing, it reschedules itself for the next day.
 */
class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "study_reminder_channel"
        const val NOTIFICATION_ID = 1001
    }

    override fun onReceive(context: Context, intent: Intent?) {
        createNotificationChannel(context)
        showNotification(context)

        // Reschedule for tomorrow
        val prefs = context.getSharedPreferences("study_settings", Context.MODE_PRIVATE)
        if (prefs.getBoolean("reminder_enabled", false)) {
            val hour = prefs.getInt("reminder_hour", 20)
            val minute = prefs.getInt("reminder_minute", 0)
            ReminderScheduler(context).schedule(hour, minute)
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Nhắc nhở ôn tập",
                NotificationManager.IMPORTANCE_HIGH  // HIGH = heads-up popup
            ).apply {
                description = "Nhắc nhở bạn ôn tập thẻ ghi nhớ hàng ngày"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(context: Context) {
        // Check permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val messages = listOf(
            "Đã đến giờ ôn tập! 📚 Hãy dành vài phút để ôn lại các thẻ nhé.",
            "Đừng quên ôn bài hôm nay! 🧠 Chỉ cần 5 phút thôi.",
            "Bạn có thẻ cần ôn tập! 🔥 Duy trì chuỗi ngày học nào!",
            "Thời gian ôn tập đây! ⏰ Bắt đầu ngay để không quên nhé."
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Smart Flashcard")
            .setContentText(messages.random())
            .setPriority(NotificationCompat.PRIORITY_HIGH)   // HIGH = heads-up
            .setDefaults(NotificationCompat.DEFAULT_ALL)       // sound + vibration
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
