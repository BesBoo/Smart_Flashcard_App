package com.example.myapplication.domain.repository

interface SyncRepository {
    /**
     * Full sync cycle: push local changes, pull remote changes.
     * @return new sync timestamp from server, or null if sync failed
     */
    suspend fun sync(userId: String, lastSyncTimestamp: String): String?

    /**
     * Get count of locally pending changes that haven't been pushed to server.
     */
    suspend fun getPendingChangesCount(userId: String, lastSyncTimestamp: String): Int
}
