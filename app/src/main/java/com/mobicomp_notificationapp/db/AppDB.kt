package com.mobicomp_notificationapp.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities=[ProfileTable::class, ReminderTable::class, LocationTable::class], version=4)
@TypeConverters(Converters::class)
abstract class AppDB : RoomDatabase() {
    abstract fun profileDAO():ProfileDAO
    abstract fun reminderDAO():ReminderDAO
    abstract fun locationDAO():LocationDAO
}