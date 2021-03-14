package com.mobicomp_notificationapp.db

import androidx.room.*
import java.util.*

@Dao
interface ReminderDAO {
    @Transaction
    @Insert
    fun insert(reminder: ReminderTable): Long

    @Update
    fun update(reminder: ReminderTable)

    @Query("SELECT id FROM reminders WHERE profile_id = :profile")
    fun getIdsByUser(profile: Int): List<Int>

    @Query("SELECT * FROM reminders WHERE profile_id = :profile")
    fun getRemindersByUser(profile: Int): List<ReminderTable>

    @Query("SELECT id FROM reminders WHERE reminder_time BETWEEN :start AND :end AND profile_id = :profile")
    fun getIdsByDate(profile: Int, start: Date, end: Date): List<Int>

    @Query("SELECT * FROM reminders WHERE reminder_time < :time AND profile_id = :profile")
    fun getRemindersBeforeDate(profile: Int, time: Date): List<ReminderTable>

    @Query("SELECT * FROM reminders WHERE reminder_time >= :time AND profile_id = :profile")
    fun getRemindersAfterDate(profile: Int, time: Date): List<ReminderTable>

    @Query("SELECT * FROM reminders WHERE id = :rem_id")
    fun getReminder(rem_id: Int): ReminderTable

    @Query("SELECT * FROM reminders")
    fun getAll(): List<ReminderTable>

    @Query("SELECT workmanager_uuid FROM reminders WHERE id = :id")
    fun getNotificationUUID(id: Int): UUID

    @Query("UPDATE reminders SET workmanager_uuid = :uuid WHERE id = :id")
    fun setNotificationUUID(id: Int, uuid: UUID)

    @Query("UPDATE reminders SET reminder_seen = :status WHERE id = :id")
    fun setSeenStatus(id: Int, status: Int)

    @Query("SELECT reminder_time FROM reminders WHERE id = :id")
    fun getTime(id: Int): Date

    @Query("DELETE FROM reminders WHERE id = :rem_id")
    fun delete(rem_id: Int)
}