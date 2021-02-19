package com.mobicomp_notificationapp.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName="reminders")
data class ReminderTable(
    @PrimaryKey(autoGenerate=true) var id: Int?,
    @ColumnInfo(name="title") var title:String,
    @ColumnInfo(name="description") var description:String,
    @ColumnInfo(name="datetime") var datetime:String
)