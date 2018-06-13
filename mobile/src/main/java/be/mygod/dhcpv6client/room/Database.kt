package be.mygod.dhcpv6client.room

import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import be.mygod.dhcpv6client.App.Companion.app

@android.arch.persistence.room.Database(entities = [InterfaceStatement::class], version = 1)
abstract class Database : RoomDatabase() {
    companion object {
        const val DB_NAME = "data.db"

        private val instance by lazy {
            Room.databaseBuilder(app.deviceContext, Database::class.java, DB_NAME)
                    .allowMainThreadQueries()
                    .build()
        }

        val interfaceStatementDao get() = instance.interfaceStatementDao()
    }

    abstract fun interfaceStatementDao(): InterfaceStatement.Dao
}
