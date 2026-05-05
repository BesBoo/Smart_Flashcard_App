package com.example.myapplication.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.myapplication.data.local.dao.AiChatDao
import com.example.myapplication.data.local.dao.DeckDao
import com.example.myapplication.data.local.dao.FlashcardDao
import com.example.myapplication.data.local.dao.ReviewLogDao
import com.example.myapplication.data.local.dao.UserDao
import com.example.myapplication.data.local.entity.AiChatEntity
import com.example.myapplication.data.local.entity.DeckEntity
import com.example.myapplication.data.local.entity.FlashcardEntity
import com.example.myapplication.data.local.entity.ReviewLogEntity
import com.example.myapplication.data.local.entity.SyncMetadataEntity
import com.example.myapplication.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        DeckEntity::class,
        FlashcardEntity::class,
        ReviewLogEntity::class,
        AiChatEntity::class,
        SyncMetadataEntity::class
    ],
    version = 5,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun deckDao(): DeckDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun reviewLogDao(): ReviewLogDao
    abstract fun aiChatDao(): AiChatDao

    companion object {
        const val DATABASE_NAME = "smart_flashcard_db"
    }
}
