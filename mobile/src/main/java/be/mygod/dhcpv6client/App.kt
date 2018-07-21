package be.mygod.dhcpv6client

import android.annotation.SuppressLint
import android.app.Application
import android.os.Build
import android.os.Handler
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
}
