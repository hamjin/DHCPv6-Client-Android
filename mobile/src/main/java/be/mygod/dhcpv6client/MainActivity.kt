package be.mygod.dhcpv6client

import android.net.Uri
import android.os.Bundle
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var serviceSwitch: Switch
    private val customTabsIntent by lazy {
        CustomTabsIntent.Builder()
                .setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .build()
    }
    fun launchUrl(url: Uri) = customTabsIntent.launchUrl(this, url)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        serviceSwitch = findViewById(R.id.service_switch)
        serviceSwitch.setOnCheckedChangeListener { _, value -> Dhcp6cService.enabled = value }
    }

    override fun onStart() {
        super.onStart()
        Dhcp6cService.enabledChanged[this] = { serviceSwitch.isChecked = it }
    }

    override fun onResume() {
        super.onResume()
        serviceSwitch.isChecked = Dhcp6cService.enabled
        Dhcp6cService.enabled = Dhcp6cService.enabled
    }

    override fun onStop() {
        Dhcp6cService.enabledChanged -= this
        super.onStop()
    }
}
