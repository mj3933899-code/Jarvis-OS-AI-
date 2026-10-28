package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.MemoryEntity
import com.example.data.entity.MessageEntity
import com.example.data.entity.ProjectEntity
import com.example.data.entity.ReceiptEntity
import com.example.data.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JarvisDao {
    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessage(message: MessageEntity): Long

    @Query("DELETE FROM messages")
    fun clearAllMessages(): Int

    @Query("SELECT * FROM tasks ORDER BY createdTime DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    fun getTaskById(taskId: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateTask(task: TaskEntity): Long

    @Query("DELETE FROM tasks WHERE id = :taskId")
    fun deleteTask(taskId: String): Int

    @Query("DELETE FROM tasks")
    fun clearAllTasks(): Int

    @Query("SELECT * FROM projects ORDER BY createdTime DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateProject(project: ProjectEntity): Long

    @Query("DELETE FROM projects WHERE id = :projectId")
    fun deleteProject(projectId: String): Int

    @Query("SELECT * FROM memories ORDER BY timestamp DESC")
    fun getAllMemories(): Flow<List<MemoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMemory(memory: MemoryEntity): Long

    @Query("DELETE FROM memories")
    fun clearAllMemories(): Int

    @Query("SELECT * FROM receipts ORDER BY timestamp DESC")
    fun getAllReceipts(): Flow<List<ReceiptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertReceipt(receipt: ReceiptEntity): Long
}
