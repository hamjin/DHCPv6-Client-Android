package be.mygod.dhcpv6client

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.core.os.postDelayed
import androidx.lifecycle.MutableLiveData
import be.mygod.dhcpv6client.App.Companion.app
import be.mygod.dhcpv6client.widget.SmartSnackbar
import java.io.IOException

class Dhcp6cService : Service() {
    companion object {
        private const val TAG = "Dhcp6cService"
        var running = false
        val enabled = MutableLiveData<Boolean>().apply {
            value = BootReceiver.enabled
            observeForever {
                val intent = Intent(app, Dhcp6cService::class.java)
                if (it && !running) {
                    app.startService(intent)
                    if (app.backgroundUnavailable)  {
                        // this block can only be reached on API 26+
                        app.startForegroundService(intent)
                    }
                } else if (!it && running) app.stopService(intent)
                BootReceiver.enabled = it
            }
        }
    }

    private val connectivity by lazy { getSystemService<ConnectivityManager>()!! }
    private val request = NetworkRequest.Builder()
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            .build()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        var registered = false
        val working = HashMap<Network, LinkProperties>()
        val reporting = HashMap<Network, Long>()

        private fun reportPeriodically(network: Network) {
            val delay = reporting[network] ?: return
            connectivity.reportNetworkConnectivity(network, true)
            if (delay < 5 * 60 * 1000) {   // if 10 mins have passed, give up
                reporting[network] = delay * 2
                app.handler.postDelayed(delay, network) { reportPeriodically(network) }
            } else reporting.remove(network)
        }

        override fun onAvailable(network: Network) {
            val prop = connectivity.getLinkProperties(network) ?: return
            onLinkPropertiesChanged(network, prop)
        }

        override fun onLinkPropertiesChanged(network: Network, link: LinkProperties) {
            val oldLink = working.put(network, link)
            val ifname = link.interfaceName
            if (ifname == null) {
                if (oldLink?.interfaceName != null) onLost(network)
                return
            }
            if (ifname != oldLink?.interfaceName) try {
                onLost(oldLink?.interfaceName)
                Dhcp6cManager.startInterface(ifname)
            } catch (e: IOException) {
                e.printStackTrace()
                if (e.message?.contains("connect:") == true) try {
                    Dhcp6cManager.forceRestartDaemon(working.values.map { it.interfaceName!! })
                } catch (e: IOException) {
                    e.printStackTrace()
                } else {
                    SmartSnackbar.make(e.localizedMessage).show()
                }
            } else if (link.linkAddresses.size > oldLink.linkAddresses.size) {
                Log.d(TAG, "Link addresses updated for $network: $oldLink => $link")
                // update connectivity on linkAddresses change
                if (true != connectivity.getNetworkCapabilities(network)?.hasCapability(
                                NetworkCapabilities.NET_CAPABILITY_VALIDATED) && !reporting.containsKey(network)) {
                    reporting[network] = 2000
                    reportPeriodically(network)
                }
            }
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            if ((networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) &&
                    reporting.remove(network) != null) app.handler.removeCallbacksAndMessages(network)
        }

        override fun onLost(network: Network) {
            onLost(working.remove(network)?.interfaceName)
            app.handler.removeCallbacksAndMessages(network)
            reporting.remove(network)
        }
        private fun onLost(ifname: String?) {
            if (ifname == null) return
            synchronized(Dhcp6cDaemon.addressLookup) {
                if (Dhcp6cDaemon.addressLookup.remove(ifname) != null) Dhcp6cDaemon.postAddressUpdate()
            }
            Dhcp6cManager.stopInterface(ifname)
        }
    }

    override fun onCreate() {
        super.onCreate()
        running = true
    }

    override fun onBind(p0: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (app.backgroundUnavailable)  {
            getSystemService<NotificationManager>()?.createNotificationChannel(
                    NotificationChannel("service", Dhcp6cManager.DHCP6C, NotificationManager.IMPORTANCE_NONE).apply {
                        lockscreenVisibility = NotificationCompat.VISIBILITY_SECRET
                    })
            startForeground(1, NotificationCompat.Builder(this, "service").run {
                priority = NotificationCompat.PRIORITY_DEFAULT
                build()
            })
        }
        if (!callback.registered) {
            try {
                connectivity.registerNetworkCallback(request, callback)
                callback.registered = true
            } catch (e: IOException) {
                SmartSnackbar.make(e.localizedMessage).show()
                e.printStackTrace()
                stopSelf(startId)
                enabled.value = false
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
        try {
            Dhcp6cManager.stopDaemonSync()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        running = false
        super.onDestroy()
    }
}
