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

    @Query("SELECT id FROM reminders WHERE reminder_time BETWEEN :start AND :end")
    fun getIdsByDate(start: Date, end: Date): List<Int>

    @Query("SELECT * FROM reminders WHERE reminder_time BETWEEN :start AND :end")
    fun getRemindersByDate(start: Date, end: Date): List<ReminderTable>

    @Query("SELECT * FROM reminders WHERE id = :rem_id")
    fun getReminder(rem_id: Int): ReminderTable

    @Query("SELECT * FROM reminders")
    fun getAll(): List<ReminderTable>

    @Query("DELETE FROM reminders WHERE id = :rem_id")
    fun delete(rem_id: Int)
}