
package com.example.crdisease.Dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.crdisease.model.DiseaseRecord


@Dao
interface DiseaseRecordDao {
    @Insert
    suspend fun insertRecord(record: DiseaseRecord)

    @Query("SELECT * FROM disease_history WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getRecordsForUser(userId: String): List<DiseaseRecord>
}
