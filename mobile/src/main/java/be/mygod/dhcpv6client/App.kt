package be.mygod.dhcpv6client

import android.annotation.SuppressLint
import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.provider.Settings
import be.mygod.dhcpv6client.room.Database
import be.mygod.dhcpv6client.util.DeviceStorageApp
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric

class App : Application() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var app: App
    }

    override fun onCreate() {
        super.onCreate()
        app = this
        if (Build.VERSION.SDK_INT >= 24) {
            deviceStorage = DeviceStorageApp(this)
            deviceStorage.moveDatabaseFrom(this, Database.DB_NAME)
        } else deviceStorage = this
        Fabric.with(deviceStorage, Crashlytics())
    }

    lateinit var deviceStorage: Application
    val handler = Handler()
    /**
     * Android TV or devices that can't opt out of Android system battery optimizations.
     */
    val backgroundUnavailable by lazy {
        if (Build.VERSION.SDK_INT < 26) return@lazy false
        when (Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).resolveActivity(packageManager)?.className) {
            "com.android.tv.settings.EmptyStubActivity", null -> true
            else -> false
        }
    }
}
