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
    CachedProductEntity::class,
    PendingProductEntity::class,
    CachedPaymentEntity::class,
    PendingPaymentEntity::class,
    CachedSupplierBillEntity::class,
    PendingSupplierBillEntity::class,
  ],
  version = 6,
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

    val MIGRATION_4_5 = object : Migration(4, 5) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `cached_products` (
            `id` TEXT NOT NULL,
            `userId` TEXT NOT NULL,
            `productCode` TEXT NOT NULL,
            `sku` TEXT NOT NULL,
            `barcode` TEXT NOT NULL,
            `name` TEXT NOT NULL,
            `category` TEXT NOT NULL,
            `brand` TEXT NOT NULL,
            `baseUnit` TEXT NOT NULL,
            `sellingUnit` TEXT NOT NULL,
            `casePack` TEXT NOT NULL,
            `packSize` TEXT NOT NULL,
            `wholesalePrice` REAL NOT NULL,
            `mrp` REAL NOT NULL,
            `stockQuantity` INTEGER NOT NULL,
            `minThreshold` INTEGER NOT NULL,
            `photoUri` TEXT NOT NULL,
            `notes` TEXT NOT NULL,
            `status` TEXT NOT NULL,
            `salesVelocity` INTEGER NOT NULL,
            `hasVariants` INTEGER NOT NULL,
            `variantsJson` TEXT NOT NULL,
            `addedDate` TEXT NOT NULL,
            `createdAtMillis` INTEGER NOT NULL,
            `updatedAtMillis` INTEGER NOT NULL,
            PRIMARY KEY(`id`)
          )
          """.trimIndent()
        )
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `pending_products` (
            `localId` TEXT NOT NULL,
            `remoteProductId` TEXT NOT NULL,
            `isUpdate` INTEGER NOT NULL,
            `userId` TEXT NOT NULL,
            `productCode` TEXT NOT NULL,
            `sku` TEXT NOT NULL,
            `barcode` TEXT NOT NULL,
            `name` TEXT NOT NULL,
            `category` TEXT NOT NULL,
            `brand` TEXT NOT NULL,
            `baseUnit` TEXT NOT NULL,
            `sellingUnit` TEXT NOT NULL,
            `casePack` TEXT NOT NULL,
            `packSize` TEXT NOT NULL,
            `wholesalePrice` REAL NOT NULL,
            `mrp` REAL NOT NULL,
            `stockQuantity` INTEGER NOT NULL,
            `minThreshold` INTEGER NOT NULL,
            `photoUri` TEXT NOT NULL,
            `notes` TEXT NOT NULL,
            `status` TEXT NOT NULL,
            `salesVelocity` INTEGER NOT NULL,
            `hasVariants` INTEGER NOT NULL,
            `variantsJson` TEXT NOT NULL,
            `addedDate` TEXT NOT NULL,
            `globalSynced` INTEGER NOT NULL,
            `lastError` TEXT NOT NULL,
            `createdAtMillis` INTEGER NOT NULL,
            `updatedAtMillis` INTEGER NOT NULL,
            PRIMARY KEY(`localId`)
          )
          """.trimIndent()
        )
      }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `cached_payments` (
            `id` TEXT NOT NULL,
            `userId` TEXT NOT NULL,
            `receiptCode` TEXT NOT NULL,
            `direction` TEXT NOT NULL,
            `partyId` TEXT NOT NULL,
            `partyCode` TEXT NOT NULL,
            `partyName` TEXT NOT NULL,
            `contactName` TEXT NOT NULL,
            `phone` TEXT NOT NULL,
            `invoiceRef` TEXT NOT NULL,
            `amount` REAL NOT NULL,
            `paymentMethod` TEXT NOT NULL,
            `referenceNumber` TEXT NOT NULL,
            `paymentDate` TEXT NOT NULL,
            `recordedBy` TEXT NOT NULL,
            `note` TEXT NOT NULL,
            `createdAtMillis` INTEGER NOT NULL,
            `updatedAtMillis` INTEGER NOT NULL,
            PRIMARY KEY(`id`)
          )
          """.trimIndent()
        )
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `pending_payments` (
            `localId` TEXT NOT NULL,
            `remotePaymentId` TEXT NOT NULL,
            `isUpdate` INTEGER NOT NULL,
            `userId` TEXT NOT NULL,
            `receiptCode` TEXT NOT NULL,
            `direction` TEXT NOT NULL,
            `partyId` TEXT NOT NULL,
            `partyCode` TEXT NOT NULL,
            `partyName` TEXT NOT NULL,
            `contactName` TEXT NOT NULL,
            `phone` TEXT NOT NULL,
            `invoiceRef` TEXT NOT NULL,
            `amount` REAL NOT NULL,
            `paymentMethod` TEXT NOT NULL,
            `referenceNumber` TEXT NOT NULL,
            `paymentDate` TEXT NOT NULL,
            `recordedBy` TEXT NOT NULL,
            `note` TEXT NOT NULL,
            `globalSynced` INTEGER NOT NULL,
            `lastError` TEXT NOT NULL,
            `createdAtMillis` INTEGER NOT NULL,
            `updatedAtMillis` INTEGER NOT NULL,
            PRIMARY KEY(`localId`)
          )
          """.trimIndent()
        )
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `cached_supplier_bills` (
            `id` TEXT NOT NULL,
            `userId` TEXT NOT NULL,
            `supplierCode` TEXT NOT NULL,
            `supplierName` TEXT NOT NULL,
            `contactPerson` TEXT NOT NULL,
            `phone` TEXT NOT NULL,
            `location` TEXT NOT NULL,
            `category` TEXT NOT NULL,
            `billNumber` TEXT NOT NULL,
            `totalBilled` REAL NOT NULL,
            `paidAmount` REAL NOT NULL,
            `billDate` TEXT NOT NULL,
            `dueDate` TEXT NOT NULL,
            `status` TEXT NOT NULL,
            `notes` TEXT NOT NULL,
            `createdAtMillis` INTEGER NOT NULL,
            `updatedAtMillis` INTEGER NOT NULL,
            PRIMARY KEY(`id`)
          )
          """.trimIndent()
        )
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `pending_supplier_bills` (
            `localId` TEXT NOT NULL,
            `remoteBillId` TEXT NOT NULL,
            `isUpdate` INTEGER NOT NULL,
            `userId` TEXT NOT NULL,
            `supplierCode` TEXT NOT NULL,
            `supplierName` TEXT NOT NULL,
            `contactPerson` TEXT NOT NULL,
            `phone` TEXT NOT NULL,
            `location` TEXT NOT NULL,
            `category` TEXT NOT NULL,
            `billNumber` TEXT NOT NULL,
            `totalBilled` REAL NOT NULL,
            `paidAmount` REAL NOT NULL,
            `billDate` TEXT NOT NULL,
            `dueDate` TEXT NOT NULL,
            `status` TEXT NOT NULL,
            `notes` TEXT NOT NULL,
            `globalSynced` INTEGER NOT NULL,
            `lastError` TEXT NOT NULL,
            `createdAtMillis` INTEGER NOT NULL,
            `updatedAtMillis` INTEGER NOT NULL,
            PRIMARY KEY(`localId`)
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
          .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
          .build()
          .also { INSTANCE = it }
      }
    }
  }
}
