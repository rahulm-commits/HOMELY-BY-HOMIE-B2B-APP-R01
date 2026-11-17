package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
  entities = [
    PendingShopEntity::class,
    CachedShopEntity::class,
    PendingOrderEntity::class,
    CachedOrderEntity::class,
    DeliveryRouteEntity::class,
    CachedStockEntity::class,
    PendingStockUpdateEntity::class,
    CachedMetaEntity::class,
  ],
  version = 4,
  exportSchema = false,
)
abstract class SnackRouteDatabase : RoomDatabase() {
  abstract fun snackRouteDao(): SnackRouteDao

  companion object {
    @Volatile
    private var INSTANCE: SnackRouteDatabase? = null

    val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `delivery_routes` (
            `id` TEXT NOT NULL,
            `name` TEXT NOT NULL,
            `vehicle` TEXT NOT NULL,
            `maxCrates` INTEGER NOT NULL,
            `driverName` TEXT NOT NULL,
            `driverPhone` TEXT NOT NULL,
            `dispatchWindow` TEXT NOT NULL,
            `stagingBay` TEXT NOT NULL,
            `colorIndex` INTEGER NOT NULL,
            `isActive` INTEGER NOT NULL,
            `updatedAtMillis` INTEGER NOT NULL,
            PRIMARY KEY(`id`)
          )
          """.trimIndent()
        )
      }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          "ALTER TABLE `delivery_routes` ADD COLUMN `stopOrderIdsCsv` TEXT NOT NULL DEFAULT ''"
        )
      }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `cached_stocks` (
            `id` TEXT NOT NULL,
            `userId` TEXT NOT NULL,
            `productCode` TEXT NOT NULL,
            `productName` TEXT NOT NULL,
            `packSize` TEXT NOT NULL,
            `category` TEXT NOT NULL,
            `quantity` INTEGER NOT NULL,
            `minThreshold` INTEGER NOT NULL,
            `unitPrice` REAL NOT NULL,
            `updatedAtMillis` INTEGER NOT NULL,
            PRIMARY KEY(`id`)
          )
          """.trimIndent()
        )
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `pending_stock_updates` (
            `stockId` TEXT NOT NULL,
            `userId` TEXT NOT NULL,
            `productName` TEXT NOT NULL,
            `newQuantity` INTEGER NOT NULL,
            `globalSynced` INTEGER NOT NULL,
            `lastError` TEXT NOT NULL,
            `updatedAtMillis` INTEGER NOT NULL,
            PRIMARY KEY(`stockId`)
          )
          """.trimIndent()
        )
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `cached_meta` (
            `userId` TEXT NOT NULL,
            `shopRegTarget` INTEGER NOT NULL,
            `vehiclesToDispatch` INTEGER NOT NULL,
            `vehiclesOnRoad` INTEGER NOT NULL,
            `returnsCollected` INTEGER NOT NULL,
            `toCollectAmount` REAL NOT NULL,
            `toCollectShops` INTEGER NOT NULL,
            `toSuppliersAmount` REAL NOT NULL,
            `toSuppliersCount` INTEGER NOT NULL,
            `seeded` INTEGER NOT NULL,
            `updatedAtMillis` INTEGER NOT NULL,
            PRIMARY KEY(`userId`)
          )
          """.trimIndent()
        )
      }
    }

    fun getInstance(context: Context): SnackRouteDatabase {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          SnackRouteDatabase::class.java,
          "snackroute_local.db",
        )
          .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
          .build()
          .also { INSTANCE = it }
      }
    }
  }
}
