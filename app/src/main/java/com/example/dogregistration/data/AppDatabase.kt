package com.example.dogregistration.data
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

// 1. Define all "tables" (entities) and the version.
@Database(entities = [DogProfile::class], version = 1, exportSchema = false)
// 2. Tell Room to use our Converters file
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    // 3. List all the DAOs (query interfaces)
    abstract fun dogDao(): DogDao

    // 4. This "singleton" pattern ensures you only ever have ONE
    //    instance of the database in your whole app, which is very important.
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dog_registration_db" // This will be the name of your local database file
                )
                    .fallbackToDestructiveMigration() // Simple migration strategy
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}