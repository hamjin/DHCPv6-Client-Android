package be.mygod.dhcpv6client

import android.os.Bundle
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var serviceSwitch: Switch

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
