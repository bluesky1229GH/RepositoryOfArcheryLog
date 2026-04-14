package com.example.archerylog.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [User::class, ArcherySession::class, SessionEnd::class, Shot::class, AiFavorite::class], version = 10, exportSchema = false)
@TypeConverters(Converters::class)
abstract class ArcheryDatabase : RoomDatabase() {
    abstract fun archeryDao(): ArcheryDao

    companion object {
        @Volatile
        private var INSTANCE: ArcheryDatabase? = null
        
        val MIGRATION_2_4 = object : Migration(2, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `users` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `username` TEXT NOT NULL, `passwordHash` TEXT NOT NULL)")
                // Create a default administrator account to adopt all legacy orphaned archery sessions
                database.execSQL("INSERT INTO `users` (`id`, `username`, `passwordHash`) VALUES (1, 'Admin', '123456')")
                
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sessions_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `userId` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `locationType` TEXT NOT NULL,
                        `distance` INTEGER NOT NULL,
                        `totalScore` INTEGER NOT NULL,
                        `totalShots` INTEGER NOT NULL,
                        FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """)
                
                database.execSQL("""
                    INSERT INTO `sessions_new` (`id`, `userId`, `timestamp`, `locationType`, `distance`, `totalScore`, `totalShots`)
                    SELECT `id`, 1, `timestamp`, `locationType`, `distance`, `totalScore`, `totalShots` FROM `sessions`
                """)
                
                database.execSQL("DROP TABLE `sessions`")
                database.execSQL("ALTER TABLE `sessions_new` RENAME TO `sessions`")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_sessions_userId` ON `sessions` (`userId`)")
            }
        }
        
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `users` ADD COLUMN `passwordHash` TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `users` ADD COLUMN `email` TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `users` ADD COLUMN `avatarUri` TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Create new table with all current fields
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sessions_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `userId` INTEGER NOT NULL, 
                        `timestamp` INTEGER NOT NULL, 
                        `locationType` TEXT NOT NULL, 
                        `distance` INTEGER NOT NULL, 
                        `totalScore` INTEGER NOT NULL DEFAULT 0, 
                        `totalShots` INTEGER NOT NULL DEFAULT 0,
                        `title` TEXT NOT NULL DEFAULT '',
                        `venue` TEXT NOT NULL DEFAULT '',
                        `weather` TEXT NOT NULL DEFAULT 'Sunny',
                        `wind` TEXT NOT NULL DEFAULT 'Low',
                        FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                """.trimIndent())

                // 2. Copy data from old table
                database.execSQL("""
                    INSERT INTO `sessions_new` (`id`, `userId`, `timestamp`, `locationType`, `distance`, `totalScore`, `totalShots`)
                    SELECT `id`, `userId`, `timestamp`, `locationType`, `distance`, `totalScore`, `totalShots` FROM `sessions`
                """.trimIndent())

                // 3. Drop old table and rename new one
                database.execSQL("DROP TABLE `sessions`")
                database.execSQL("ALTER TABLE `sessions_new` RENAME TO `sessions`")
                
                // 4. Re-create index
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_sessions_userId` ON `sessions` (`userId`)")
            }
        }


        fun getDatabase(context: Context): ArcheryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ArcheryDatabase::class.java,
                    "archery_database"
                )
                .addMigrations(MIGRATION_2_4, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
