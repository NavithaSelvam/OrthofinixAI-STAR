package com.example.orthofinixai.data.local.dao

import androidx.room.*
import com.example.orthofinixai.data.local.entity.PatientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientDao {
    @Query("SELECT * FROM patients WHERE userId = :userId ORDER BY createdAt DESC")
    fun getPatientsForUser(userId: String): Flow<List<PatientEntity>>

    @Query("SELECT * FROM patients WHERE userId = :userId AND id = :id")
    suspend fun getPatient(userId: String, id: String): PatientEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatient(patient: PatientEntity)

    @Update
    suspend fun updatePatient(patient: PatientEntity)

    @Delete
    suspend fun deletePatient(patient: PatientEntity)

    @Query("SELECT * FROM patients WHERE userId = :userId AND name LIKE '%' || :query || '%'")
    fun searchPatients(userId: String, query: String): Flow<List<PatientEntity>>
}
