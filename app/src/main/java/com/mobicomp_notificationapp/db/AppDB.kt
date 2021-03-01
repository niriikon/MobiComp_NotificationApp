package com.mobicomp_notificationapp.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities=[ProfileTable::class, ReminderTable::class], version=2)
abstract class AppDB : RoomDatabase() {
    abstract fun profileDAO():ProfileDAO
    abstract fun reminderDAO():ReminderDAO
}