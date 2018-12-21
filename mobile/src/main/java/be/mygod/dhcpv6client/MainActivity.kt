package be.mygod.dhcpv6client

import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Bundle
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import be.mygod.dhcpv6client.widget.SmartSnackbar
import com.crashlytics.android.Crashlytics

class MainActivity : AppCompatActivity() {
    private lateinit var serviceSwitch: Switch
    private val customTabsIntent by lazy {
        CustomTabsIntent.Builder()
                .setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .build()
    }
    fun launchUrl(url: Uri) = if (packageManager.hasSystemFeature("android.hardware.faketouch")) try {
        customTabsIntent.launchUrl(this, url)
    } catch (e: ActivityNotFoundException) {
        e.printStackTrace()
        Crashlytics.logException(e)
        SmartSnackbar.make(url.toString()).show()
    } catch (e: SecurityException) {
        e.printStackTrace()
        Crashlytics.logException(e)
        SmartSnackbar.make(url.toString()).show()
    } else SmartSnackbar.make(url.toString()).show()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        serviceSwitch = findViewById(R.id.service_switch)
        serviceSwitch.setOnCheckedChangeListener { _, value -> Dhcp6cService.enabled = value }
        SmartSnackbar.Register(lifecycle, findViewById(R.id.fragment_holder))
    }

    override fun onStart() {
        super.onStart()
        Dhcp6cService.enabledChanged[this] = { serviceSwitch.isChecked = it }
    }

    override fun onResume() {
        super.onResume()
        Dhcp6cService.enabled = Dhcp6cService.enabled
    }

    override fun onStop() {
        Dhcp6cService.enabledChanged -= this
        super.onStop()
    }
}
