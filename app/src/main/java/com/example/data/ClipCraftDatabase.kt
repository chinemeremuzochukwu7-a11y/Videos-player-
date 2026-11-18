package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface ExportRecordDao {
  @Query("SELECT * FROM export_records ORDER BY createdAtEpochMs DESC")
  fun observeAll(): Flow<List<ExportRecordEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(record: ExportRecordEntity): Long

  @Query("DELETE FROM export_records WHERE id = :id")
  suspend fun deleteById(id: Long)
}

@Dao
interface LibraryVideoDao {
  @Query("SELECT * FROM library_videos ORDER BY deviceTimestampMs DESC")
  fun observeAll(): Flow<List<LibraryVideoEntity>>

  @Query("SELECT * FROM library_videos")
  suspend fun getAll(): List<LibraryVideoEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAll(videos: List<LibraryVideoEntity>)

  @Query("DELETE FROM library_videos WHERE uriString = :uriString")
  suspend fun deleteByUri(uriString: String)
}

@Dao
interface LinkedFolderDao {
  @Query("SELECT * FROM linked_folders ORDER BY linkedAtEpochMs DESC")
  fun observeAll(): Flow<List<LinkedFolderEntity>>

  @Query("SELECT * FROM linked_folders")
  suspend fun getAll(): List<LinkedFolderEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(folder: LinkedFolderEntity)

  @Query("DELETE FROM linked_folders WHERE treeUriString = :treeUri")
  suspend fun deleteByUri(treeUri: String)
}

@Database(
  entities = [ExportRecordEntity::class, LibraryVideoEntity::class, LinkedFolderEntity::class],
  version = 3,
  exportSchema = false,
)
abstract class ClipCraftDatabase : RoomDatabase() {
  abstract fun exportRecordDao(): ExportRecordDao

  abstract fun libraryVideoDao(): LibraryVideoDao

  abstract fun linkedFolderDao(): LinkedFolderDao

  companion object {
    @Volatile private var INSTANCE: ClipCraftDatabase? = null

    fun getInstance(context: Context): ClipCraftDatabase {
      return INSTANCE
        ?: synchronized(this) {
          INSTANCE
            ?: Room.databaseBuilder(
                context.applicationContext,
                ClipCraftDatabase::class.java,
                "clipcraft_studio.db",
              )
              .fallbackToDestructiveMigration(dropAllTables = true)
              .build()
              .also { INSTANCE = it }
        }
    }
  }
}
