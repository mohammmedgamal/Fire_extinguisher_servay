package com.powerplant.firesurvey.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SurveyDao {

    @Query(
        """
        SELECT e.*,
               i.timestamp AS lastTimestamp,
               i.status    AS lastStatus,
               i.issues    AS lastIssues,
               i.inspector AS lastInspector
        FROM extinguishers e
        LEFT JOIN inspections i ON i.id = (
            SELECT id FROM inspections WHERE extinguisherId = e.id ORDER BY timestamp DESC, id DESC LIMIT 1
        )
        ORDER BY e.name COLLATE NOCASE
        """
    )
    fun observeSummaries(): Flow<List<ExtinguisherSummary>>

    @Query("SELECT * FROM extinguishers WHERE id = :id")
    fun observeExtinguisher(id: String): Flow<Extinguisher?>

    @Query("SELECT * FROM extinguishers WHERE id = :id")
    suspend fun getExtinguisher(id: String): Extinguisher?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExtinguisher(extinguisher: Extinguisher)

    @Update
    suspend fun updateExtinguisher(extinguisher: Extinguisher)

    @Delete
    suspend fun deleteExtinguisher(extinguisher: Extinguisher)

    @Query("SELECT * FROM inspections WHERE extinguisherId = :id ORDER BY timestamp DESC, id DESC")
    fun observeInspections(id: String): Flow<List<Inspection>>

    @Insert
    suspend fun insertInspection(inspection: Inspection): Long

    @Query("SELECT * FROM inspections ORDER BY timestamp DESC, id DESC")
    suspend fun allInspections(): List<Inspection>

    @Query("SELECT * FROM extinguishers")
    suspend fun allExtinguishers(): List<Extinguisher>
}
