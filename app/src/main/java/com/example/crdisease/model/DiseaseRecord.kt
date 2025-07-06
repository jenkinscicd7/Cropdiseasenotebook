package com.example.crdisease.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "disease_history",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE // or SET_NULL, RESTRICT, etc.
        )
    ], indices = [Index(value = ["userId"])]
)
data class DiseaseRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,  // associate with logged-in user
    val disease: String,
    val recommendation: String,
    val timestamp: Long
)
