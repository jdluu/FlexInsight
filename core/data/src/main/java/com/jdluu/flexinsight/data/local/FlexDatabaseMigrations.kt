package com.jdluu.flexinsight.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room migrations. Version 2 adds [Workout.isDeleted] for Hevy delete events.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE workouts ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0"
        )
    }
}
