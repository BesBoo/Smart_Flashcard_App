package com.example.myapplication.domain.usecase

import com.example.myapplication.domain.repository.SyncRepository
import javax.inject.Inject

class SyncDataUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    /**
     * @return the new sync timestamp, or null if sync failed
     */
    suspend operator fun invoke(userId: String, lastSyncTimestamp: String): String? {
        return syncRepository.sync(userId, lastSyncTimestamp)
    }
}
