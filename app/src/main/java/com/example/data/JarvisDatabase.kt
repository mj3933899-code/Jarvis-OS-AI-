package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.JarvisDao
import com.example.data.entity.MemoryEntity
import com.example.data.entity.MessageEntity
import com.example.data.entity.ProjectEntity
import com.example.data.entity.ReceiptEntity
import com.example.data.entity.TaskEntity

@Database(
    entities = [
        MessageEntity::class,
        TaskEntity::class,
        ProjectEntity::class,
        MemoryEntity::class,
        ReceiptEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class JarvisDatabase : RoomDatabase() {
    abstract fun jarvisDao(): JarvisDao

    companion object {
        @Volatile
        private var INSTANCE: JarvisDatabase? = null

        fun getInstance(context: Context): JarvisDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    JarvisDatabase::class.java,
                    "jarvis_companion.db"
                )
                .fallbackToDestructiveMigration()
                .build().also { INSTANCE = it }
            }
        }
    }
}
