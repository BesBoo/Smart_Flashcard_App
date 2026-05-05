package com.example.myapplication.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE users ADD COLUMN role TEXT NOT NULL DEFAULT 'user'")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE decks ADD COLUMN isOwner INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE decks ADD COLUMN permission TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE decks ADD COLUMN ownerName TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE decks ADD COLUMN shareCode TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE decks ADD COLUMN isShared INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE decks ADD COLUMN googleSheetUrl TEXT DEFAULT NULL")
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE flashcards ADD COLUMN pronunciationIpa TEXT DEFAULT NULL")
        }
    }
}
