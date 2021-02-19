package com.mobicomp_notificationapp.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName="profiles")
data class ProfileTable(
    @PrimaryKey(autoGenerate=true) var uid: Int?,
    @ColumnInfo(name="username") var username:String,
    @ColumnInfo(name="password") var password:String,
    @ColumnInfo(name="realname") var realname:String
)