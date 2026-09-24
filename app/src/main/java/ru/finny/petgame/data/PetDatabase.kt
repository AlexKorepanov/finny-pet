package ru.finny.petgame.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ru.finny.petgame.data.dao.BalanceDao
import ru.finny.petgame.data.dao.BudgetPlanItemDao
import ru.finny.petgame.data.dao.CompletedTaskDao
import ru.finny.petgame.data.dao.EarningDao
import ru.finny.petgame.data.dao.PeriodDao
import ru.finny.petgame.data.dao.ProfileDao
import ru.finny.petgame.data.dao.ProgressDao
import ru.finny.petgame.data.dao.PurchaseDao
import ru.finny.petgame.data.dao.SavingsDao
import ru.finny.petgame.data.dao.SavingsOperationDao
import ru.finny.petgame.data.entity.BalanceEntity
import ru.finny.petgame.data.entity.BudgetPlanItemEntity
import ru.finny.petgame.data.entity.CompletedTaskEntity
import ru.finny.petgame.data.entity.EarningEntity
import ru.finny.petgame.data.entity.PeriodEntity
import ru.finny.petgame.data.entity.ProfileEntity
import ru.finny.petgame.data.entity.ProgressEntity
import ru.finny.petgame.data.entity.PurchaseEntity
import ru.finny.petgame.data.entity.SavingsEntity
import ru.finny.petgame.data.entity.SavingsOperationEntity

@Database(
    entities = [
        ProfileEntity::class,
        BalanceEntity::class,
        PurchaseEntity::class,
        SavingsEntity::class,
        ProgressEntity::class,
        EarningEntity::class,
        PeriodEntity::class,
        BudgetPlanItemEntity::class,
        SavingsOperationEntity::class,
        CompletedTaskEntity::class,
    ],
    version = 7,
    exportSchema = false,
)
abstract class PetDatabase : RoomDatabase() {

    abstract fun profileDao(): ProfileDao
    abstract fun balanceDao(): BalanceDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun savingsDao(): SavingsDao
    abstract fun progressDao(): ProgressDao
    abstract fun earningDao(): EarningDao
    abstract fun periodDao(): PeriodDao
    abstract fun budgetPlanItemDao(): BudgetPlanItemDao
    abstract fun savingsOperationDao(): SavingsOperationDao
    abstract fun completedTaskDao(): CompletedTaskDao

    companion object {
        private const val DB_NAME = "finny_pet.db"

        @Volatile
        private var instance: PetDatabase? = null

        fun getInstance(context: Context): PetDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PetDatabase::class.java,
                    DB_NAME,
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
    }
}