package com.attendtrack.telangana

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val attended: Int = 0,
    val held: Int = 0
)

@Entity(
    tableName = "records",
    indices = [Index(value = ["date", "period"], unique = true)]
)
data class AttRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectId: Int,
    val date: String,
    val period: Int,
    val present: Boolean
)

@Entity(tableName = "holidays")
data class Holiday(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val startDate: String,
    val endDate: String,
    val provisional: Boolean
)

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY id")
    fun all(): Flow<List<Subject>>

    @Query("SELECT * FROM subjects ORDER BY id")
    suspend fun allSubjects(): List<Subject>

    @Query("SELECT COUNT(*) FROM subjects")
    suspend fun count(): Int

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun getSubject(id: Int): Subject

    @Insert
    suspend fun insertAll(list: List<Subject>)

    @Update
    suspend fun update(s: Subject)

    @Query("DELETE FROM subjects")
    suspend fun clearSubjects()

    @Query("SELECT * FROM records WHERE date = :date")
    fun forDate(date: String): Flow<List<AttRecord>>

    @Query("SELECT * FROM records WHERE date = :date AND period = :period")
    suspend fun find(date: String, period: Int): AttRecord?

    @Insert
    suspend fun insertRecord(r: AttRecord)

    @Insert
    suspend fun insertRecords(list: List<AttRecord>)

    @Update
    suspend fun updateRecord(r: AttRecord)

    @Delete
    suspend fun deleteRecord(r: AttRecord)

    @Query("SELECT * FROM records ORDER BY date, period")
    suspend fun allRecords(): List<AttRecord>

    @Query("DELETE FROM records")
    suspend fun clearRecords()

    @Query("SELECT * FROM holidays ORDER BY startDate")
    fun holidays(): Flow<List<Holiday>>

    @Query("SELECT * FROM holidays ORDER BY startDate")
    suspend fun allHolidays(): List<Holiday>

    @Query("SELECT COUNT(*) FROM holidays")
    suspend fun holidayCount(): Int

    @Insert
    suspend fun insertHoliday(h: Holiday)

    @Insert
    suspend fun insertHolidays(list: List<Holiday>)

    @Delete
    suspend fun deleteHoliday(h: Holiday)

    @Query("DELETE FROM holidays")
    suspend fun clearHolidays()
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS holidays (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "name TEXT NOT NULL, startDate TEXT NOT NULL, " +
                "endDate TEXT NOT NULL, provisional INTEGER NOT NULL)"
        )
    }
}

@Database(
    entities = [Subject::class, AttRecord::class, Holiday::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): SubjectDao

    companion object {
        @Volatile private var inst: AppDatabase? = null
        fun get(ctx: Context): AppDatabase = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(
                ctx.applicationContext, AppDatabase::class.java, "attendtrack.db"
            ).addMigrations(MIGRATION_3_4).build().also { inst = it }
        }
    }
}
