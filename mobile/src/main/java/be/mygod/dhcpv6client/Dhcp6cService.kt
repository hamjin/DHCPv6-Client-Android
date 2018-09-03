package be.mygod.dhcpv6client

import android.annotation.TargetApi
import android.app.*
import android.content.Intent
import android.net.*
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import be.mygod.dhcpv6client.App.Companion.app
import be.mygod.dhcpv6client.util.StickyEvent1
import be.mygod.dhcpv6client.widget.SmartSnackbar
import com.crashlytics.android.Crashlytics
import java.io.IOException

class Dhcp6cService : Service() {
    companion object {
        var running = false
        var enabled: Boolean
            get() = BootReceiver.enabled
            set(value) {
                val intent = Intent(app, Dhcp6cService::class.java)
                if (value && !Dhcp6cService.running) {
                    if (app.backgroundUnavailable) @TargetApi(26) {
                        // this block can only be reached on API 26+
                        app.startForegroundService(intent)
                    } else app.startService(intent)
                }
                else if (!value && Dhcp6cService.running) app.stopService(intent)
                if (value == BootReceiver.enabled) return
                BootReceiver.enabled = value
                enabledChanged(value)
            }
        val enabledChanged = StickyEvent1 { enabled }
    }

    private val connectivity by lazy { getSystemService<ConnectivityManager>()!! }
    private val request = NetworkRequest.Builder()
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            .build()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        var registered = false
        val working = HashMap<Network, LinkProperties>()

        private fun <T> List<T>.isSameAs(other: List<T>) = size == other.size && zip(other).all { (a, b) -> a == b }

        override fun onLinkPropertiesChanged(network: Network, link: LinkProperties?) {
            val ifname = link?.interfaceName ?: return
            val oldLink = working.put(network, link)
            if (oldLink == null) try { // prevent re-requesting the same interface
                Dhcp6cManager.startInterface(ifname)
            } catch (e: IOException) {
                e.printStackTrace()
                if (e.message?.contains("connect: Connection refused") == true) {
                    Dhcp6cManager.forceRestartDaemon(working.values.map { it.interfaceName })
                } else SmartSnackbar.make(e.localizedMessage).show()
                Crashlytics.logException(e)
            } else if (!link.linkAddresses.isSameAs(oldLink.linkAddresses)) {
                // update connectivity on linkAddresses change
                Crashlytics.log(Log.INFO, "Dhcp6cService", "Reporting connectivity on network $network ($link)")
                if (Build.VERSION.SDK_INT >= 23) connectivity.reportNetworkConnectivity(network, true)
                else @Suppress("DEPRECATION") connectivity.reportBadNetwork(network)
            }
        }

        override fun onLost(network: Network?) {
            Dhcp6cManager.stopInterface(working.remove(network ?: return)?.interfaceName ?: return)
        }
    }

    override fun onCreate() {
        super.onCreate()
        running = true
    }

    override fun onBind(p0: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (app.backgroundUnavailable) @TargetApi(26) {
            getSystemService<NotificationManager>()?.createNotificationChannel(
                    NotificationChannel("service", Dhcp6cManager.DHCP6C, NotificationManager.IMPORTANCE_NONE).apply {
                        lockscreenVisibility = NotificationCompat.VISIBILITY_SECRET
                    })
            startForeground(1, NotificationCompat.Builder(this, "service").run {
                priority = NotificationCompat.PRIORITY_LOW
                build()
            })
        }
        if (!callback.registered) {
            try {
                connectivity.registerNetworkCallback(request, callback)
                callback.registered = true
            } catch (e: IOException) {
                SmartSnackbar.make(e.localizedMessage).show()
                Crashlytics.logException(e)
                e.printStackTrace()
                stopSelf(startId)
                enabled = false
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        if (callback.registered) {
            connectivity.unregisterNetworkCallback(callback)
            callback.working.clear()
            callback.registered = false
        }
        Dhcp6cManager.stopDaemonSync()
        running = false
        super.onDestroy()
    }
}
