package com.example.crdisease.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.crdisease.Dao.DiseaseRecordDao
import com.example.crdisease.Dao.UserDao
import com.example.crdisease.model.DiseaseRecord
import com.example.crdisease.model.User

@Database(entities = [User::class, DiseaseRecord::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {

    abstract fun diseaseRecordDao(): DiseaseRecordDao
    abstract fun userDao(): UserDao


    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}


