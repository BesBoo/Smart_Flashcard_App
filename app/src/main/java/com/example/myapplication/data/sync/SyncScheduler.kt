package com.example.myapplication.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules and manages background sync via WorkManager.
 * Provides observable sync status for UI indicators.
 */
@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    /**
     * Schedules a periodic sync every 30 minutes.
     * Only runs when network is connected.
     */
    fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            repeatInterval = 30,
            repeatIntervalTimeUnit = TimeUnit.MINUTES,
            flexTimeInterval = 10,
            flexTimeIntervalUnit = TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .addTag(SyncWorker.TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    /**
     * Cancels periodic sync (e.g. on logout).
     */
    fun cancelPeriodicSync() {
        workManager.cancelUniqueWork(SyncWorker.WORK_NAME)
    }

    /**
     * Observable sync status for UI.
     * Emits true when sync is currently RUNNING.
     */
    val isSyncing: Flow<Boolean> = workManager
        .getWorkInfosForUniqueWorkFlow(SyncWorker.WORK_NAME)
        .map { workInfos ->
            workInfos.any { it.state == WorkInfo.State.RUNNING }
        }
}
