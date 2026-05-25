package com.example.orthofinixai.data.local.dao

import androidx.room.*
import com.example.orthofinixai.data.local.entity.CaseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CaseDao {
    @Query("SELECT * FROM cases WHERE userId = :userId ORDER BY createdAt DESC")
    fun getCasesForUser(userId: String): Flow<List<CaseEntity>>

    @Query("SELECT * FROM cases WHERE userId = :userId AND id = :id")
    suspend fun getCase(userId: String, id: String): CaseEntity?

    @Query("SELECT * FROM cases WHERE id = :id LIMIT 1")
    suspend fun getCaseById(id: String): CaseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCase(caseEntity: CaseEntity)

    @Query("DELETE FROM cases WHERE userId = :userId AND id = :id")
    suspend fun deleteCase(userId: String, id: String)

    @Query("SELECT * FROM cases WHERE userId = :userId AND (patientName LIKE '%' || :q || '%' OR id LIKE '%' || :q || '%') ORDER BY createdAt DESC")
    fun searchCases(userId: String, q: String): Flow<List<CaseEntity>>
}
