package com.example.myapplication.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Worker that fires a daily study reminder notification.
 * Scheduled via ReminderScheduler using WorkManager.
 */
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val CHANNEL_ID = "study_reminder_channel"
        const val NOTIFICATION_ID = 1001
        const val WORK_NAME = "daily_study_reminder"
        const val TAG = "ReminderWorker"
    }

    override suspend fun doWork(): Result {
        createNotificationChannel()
        showNotification()
        return Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Nhắc nhở ôn tập",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Nhắc nhở bạn ôn tập thẻ ghi nhớ hàng ngày"
            }
            val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun showNotification() {
        // Check permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return // No permission, skip
            }
        }

        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val messages = listOf(
            "Đã đến giờ ôn tập! 📚 Hãy dành vài phút để ôn lại các thẻ nhé.",
            "Đừng quên ôn bài hôm nay! 🧠 Chỉ cần 5 phút thôi.",
            "Bạn có thẻ cần ôn tập! 🔥 Duy trì chuỗi ngày học nào!",
            "Thời gian ôn tập đây! ⏰ Bắt đầu ngay để không quên nhé."
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Smart Flashcard")
            .setContentText(messages.random())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(appContext).notify(NOTIFICATION_ID, notification)
    }
}
