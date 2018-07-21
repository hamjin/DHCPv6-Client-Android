package be.mygod.dhcpv6client

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Handler
import android.preference.PreferenceManager
import be.mygod.dhcpv6client.room.Database

class App : Application() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var app: App
    }

    override fun onCreate() {
        super.onCreate()
        app = this
        if (Build.VERSION.SDK_INT >= 24) {
            deviceContext = createDeviceProtectedStorageContext()
            deviceContext.moveDatabaseFrom(this, Database.DB_NAME)
            deviceContext.moveSharedPreferencesFrom(this, PreferenceManager.getDefaultSharedPreferencesName(this))
        } else deviceContext = this
    }

    lateinit var deviceContext: Context
    val pref: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(deviceContext) }
    val handler = Handler()
}
