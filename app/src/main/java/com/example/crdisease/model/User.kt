package com.example.crdisease.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val address: String,
    val password: String,
    val createdAt: Long = System.currentTimeMillis()

)

