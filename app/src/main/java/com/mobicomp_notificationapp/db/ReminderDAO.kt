package com.mobicomp_notificationapp.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface ReminderDAO {
    @Transaction
    @Insert
    fun insert(reminder: ReminderTable): Long

    @Query("SELECT id FROM reminders WHERE title = :rem_title")
    fun getIdByTitle(rem_title: String): Int

    @Query("SELECT id FROM reminders WHERE datetime BETWEEN :start AND :end")
    fun getIdByDate(start: String, end: String): Int

    @Query("SELECT * FROM reminders WHERE id = :rem_id")
    fun getReminder(rem_id: Int): ReminderTable

    @Query("SELECT * FROM reminders")
    fun getAll(): List<ReminderTable>

    @Query("SELECT title FROM reminders")
    fun getTitles(): List<String>

    @Query("DELETE FROM reminders WHERE id = :rem_id")
    fun delete(rem_id: Int)
}