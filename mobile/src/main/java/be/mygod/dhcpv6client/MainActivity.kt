package be.mygod.dhcpv6client

import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Bundle
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import be.mygod.dhcpv6client.widget.SmartSnackbar

class MainActivity : AppCompatActivity() {
    private lateinit var serviceSwitch: Switch
    private val customTabsIntent by lazy {
        CustomTabsIntent.Builder()
                .setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .build()
    }
    fun launchUrl(url: Uri) {
        if (packageManager.hasSystemFeature("android.hardware.faketouch")) try {
            customTabsIntent.launchUrl(this, url)
            return
        } catch (_: ActivityNotFoundException) { } catch (_: SecurityException) { }
        SmartSnackbar.make(url.toString()).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        serviceSwitch = findViewById(R.id.service_switch)
        serviceSwitch.setOnCheckedChangeListener { _, value -> Dhcp6cService.enabled.value = value }
        SmartSnackbar.Register(lifecycle, findViewById(R.id.fragment_holder))
        Dhcp6cService.enabled.observe(this, Observer { serviceSwitch.isChecked = it })
    }

    override fun onResume() {
        super.onResume()
        Dhcp6cService.enabled.value = Dhcp6cService.enabled.value
    }
}
