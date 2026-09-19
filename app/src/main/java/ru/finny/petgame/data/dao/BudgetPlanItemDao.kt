package ru.finny.petgame.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.finny.petgame.data.entity.BudgetPlanItemEntity

@Dao
interface BudgetPlanItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<BudgetPlanItemEntity>)

    @Query("SELECT * FROM budget_plan_items WHERE periodId = :periodId")
    suspend fun getByPeriod(periodId: Long): List<BudgetPlanItemEntity>

    @Query("SELECT * FROM budget_plan_items WHERE periodId = :periodId")
    fun observeByPeriod(periodId: Long): Flow<List<BudgetPlanItemEntity>>

    @Query("DELETE FROM budget_plan_items WHERE periodId = :periodId")
    suspend fun deleteByPeriodId(periodId: Long)
}