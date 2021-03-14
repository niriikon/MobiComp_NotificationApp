package com.mobicomp_notificationapp.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities=[ProfileTable::class, ReminderTable::class], version=1)
@TypeConverters(Converters::class)
abstract class AppDB : RoomDatabase() {
    abstract fun profileDAO():ProfileDAO
    abstract fun reminderDAO():ReminderDAO
}