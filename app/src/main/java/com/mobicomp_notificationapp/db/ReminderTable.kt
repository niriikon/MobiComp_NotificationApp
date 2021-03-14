package com.mobicomp_notificationapp.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.*

@Entity(
    tableName="reminders",
    foreignKeys = arrayOf(ForeignKey(
        entity = ProfileTable::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("profile_id"),
        onDelete = ForeignKey.CASCADE)))
data class ReminderTable(
    @PrimaryKey(autoGenerate=true) var id: Int?,
    @ColumnInfo(name="profile_id") var profile_id: Int,
    @ColumnInfo(name="message") var message:String,
    @ColumnInfo(name="location_x") var location_x:String?,
    @ColumnInfo(name="location_y") var location_y:String?,
    @ColumnInfo(name="reminder_time") var reminder_time:Date,
    @ColumnInfo(name="creation_time") var creation_time: Date,
    @ColumnInfo(name="reminder_seen") var reminder_seen:Int,
    @ColumnInfo(name="icon") var icon: Int?
)
