package com.bignerdranch.android.smartwatches

import android.util.Log
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface UserDao {
    @Insert
    suspend fun insert(user: User)

    @Query("SELECT * FROM users WHERE login = :login LIMIT 1")
    suspend fun findByLogin(login: String): User?

    @Update
    suspend fun updateUser(user: User)

    @Query("SELECT stepsCount FROM users WHERE login = :login LIMIT 1")
    suspend fun getStepsCountForUser(login: String): Int
}