package be.mygod.dhcpv6client

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Switch

class MainActivity : AppCompatActivity() {
    private lateinit var serviceSwitch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        serviceSwitch = findViewById(R.id.service_switch)
        serviceSwitch.setOnCheckedChangeListener { _, value -> setServiceEnabled(value) }
    }

    override fun onResume() {
        super.onResume()
        serviceSwitch.isChecked = BootReceiver.enabled
        setServiceEnabled(BootReceiver.enabled)
    }

    private fun setServiceEnabled(enabled: Boolean) {
        BootReceiver.enabled = enabled
        if (enabled && !Dhcp6cService.running) startService(Intent(this, Dhcp6cService::class.java))
        else if (!enabled && Dhcp6cService.running) stopService(Intent(this, Dhcp6cService::class.java))
    }
}
