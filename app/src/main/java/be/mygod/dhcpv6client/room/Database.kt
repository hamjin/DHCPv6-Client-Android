package be.mygod.dhcpv6client.room

import androidx.room.Room
import androidx.room.RoomDatabase
import be.mygod.dhcpv6client.App.Companion.app

@androidx.room.Database(entities = [InterfaceStatement::class], version = 1)
abstract class Database : RoomDatabase() {
    companion object {
        const val DB_NAME = "data.db"

        private val instance by lazy {
            Room.databaseBuilder(app.deviceStorage, Database::class.java, DB_NAME)
                    .allowMainThreadQueries()
                    .build()
        }

        val interfaceStatementDao get() = instance.interfaceStatementDao()
    }

    abstract fun interfaceStatementDao(): InterfaceStatement.Dao
}
