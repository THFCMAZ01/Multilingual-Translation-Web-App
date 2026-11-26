package com.thfcmaz01.linguaswift.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "translations")
data class TranslationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceText: String,
    val translatedText: String,
    val sourceCode: String,
    val targetCode: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isFavourite: Boolean = false
)

@Dao
interface TranslationDao {
    @Query("SELECT * FROM translations ORDER BY createdAt DESC")
    fun history(): Flow<List<TranslationEntity>>
    @Query("SELECT * FROM translations WHERE isFavourite = 1 ORDER BY createdAt DESC")
    fun favourites(): Flow<List<TranslationEntity>>
    @Query("SELECT * FROM translations WHERE id = :id LIMIT 1")
    suspend fun find(id: Long): TranslationEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TranslationEntity): Long
    @Query("UPDATE translations SET isFavourite = :saved WHERE id = :id")
    suspend fun favourite(id: Long, saved: Boolean)
    @Query("DELETE FROM translations WHERE id = :id")
    suspend fun delete(id: Long)
}

@Database(entities = [TranslationEntity::class], version = 1, exportSchema = false)
abstract class LinguaSwiftDatabase : RoomDatabase() {
    abstract fun dao(): TranslationDao
}
