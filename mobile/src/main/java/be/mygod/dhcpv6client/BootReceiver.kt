package be.mygod.dhcpv6client

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.getSystemService
import be.mygod.dhcpv6client.App.Companion.app

class BootReceiver : BroadcastReceiver() {
    companion object {
        private val componentName by lazy { ComponentName(app, BootReceiver::class.java) }
        var enabled: Boolean
            get() = when (app.packageManager.getComponentEnabledSetting(componentName)) {
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                else -> false
            }
            set(value) = app.packageManager.setComponentEnabledSetting(componentName,
                    if (value) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    else PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
    }

    override fun onReceive(context: Context, intent: Intent) {
        context.apply {
            if (Build.VERSION.SDK_INT < 26 ||
                            getSystemService<PowerManager>()?.isIgnoringBatteryOptimizations(packageName) != false) {
                startService(Intent(this, Dhcp6cService::class.java))
            }
        }
    }
}
