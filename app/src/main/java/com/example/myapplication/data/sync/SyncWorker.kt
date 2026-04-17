package com.example.myapplication.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myapplication.data.remote.TokenManager
import com.example.myapplication.domain.repository.SyncRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Background WorkManager worker that performs delta sync.
 * Runs periodically (every 30 min) and on network reconnection.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncRepository: SyncRepository,
    private val tokenManager: TokenManager
) : CoroutineWorker(context, params) {

    companion object {
        const val TAG = "SyncWorker"
        const val WORK_NAME = "smart_flashcard_sync"
    }

    override suspend fun doWork(): Result {
        return try {
            val userId = tokenManager.getUserId() ?: return Result.failure()
            val lastSync = inputData.getString("lastSyncTimestamp") ?: "0"

            Log.d(TAG, "Starting background sync for user $userId since $lastSync")

            val newTimestamp = syncRepository.sync(userId, lastSync)
            if (newTimestamp != null) {
                Log.d(TAG, "Sync completed. New timestamp: $newTimestamp")
                Result.success()
            } else {
                Log.w(TAG, "Sync returned null timestamp, will retry")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed: ${e.message}")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
