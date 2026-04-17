package com.example.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.local.entity.AiChatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiChatDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: AiChatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<AiChatEntity>)

    // Load conversation in order
    @Query("SELECT * FROM ai_chat_history WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    fun observeMessagesBySession(sessionId: String): Flow<List<AiChatEntity>>

    @Query("SELECT * FROM ai_chat_history WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    suspend fun getMessagesBySession(sessionId: String): List<AiChatEntity>

    // List all sessions for a user (latest message per session)
    @Query("""
        SELECT * FROM ai_chat_history 
        WHERE userId = :userId 
        GROUP BY sessionId 
        HAVING createdAt = MAX(createdAt)
        ORDER BY createdAt DESC
    """)
    fun observeSessionsByUser(userId: String): Flow<List<AiChatEntity>>

    @Query("DELETE FROM ai_chat_history WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Query("DELETE FROM ai_chat_history WHERE userId = :userId")
    suspend fun deleteAllByUser(userId: String)
}
