package com.mobicomp_notificationapp.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName="locations",
    foreignKeys = arrayOf(ForeignKey(
        entity = ProfileTable::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("profile_id"),
        onDelete = ForeignKey.CASCADE)))
data class LocationTable(
    @PrimaryKey(autoGenerate=true) var id: Int?,
    @ColumnInfo(name="profile_id") var profile_id: Int,
    @ColumnInfo(name="name") var name: String,
    @ColumnInfo(name="latitude") var latitude:Double,
    @ColumnInfo(name="longitude") var longitude:Double
)