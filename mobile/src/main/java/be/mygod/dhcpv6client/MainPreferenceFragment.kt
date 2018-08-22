package be.mygod.dhcpv6client

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import be.mygod.dhcpv6client.App.Companion.app

class MainPreferenceFragment : PreferenceFragmentCompat() {
    private lateinit var batteryKiller: SwitchPreference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_main)
        batteryKiller = findPreference("service.batteryKiller") as SwitchPreference
        batteryKiller.setOnPreferenceChangeListener { _, _ ->
            if (Build.VERSION.SDK_INT < 23) return@setOnPreferenceChangeListener false
            val context = requireContext()
            startActivity(if (batteryKiller.isChecked && ContextCompat.checkSelfPermission(context,
                            Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) ==
                    PackageManager.PERMISSION_GRANTED)
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                        .setData("package:${context.packageName}".toUri())
            else Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            false
        }
        if (Build.VERSION.SDK_INT >= 26 && !app.backgroundUnavailable)
            batteryKiller.setSwitchTextOn(R.string.settings_service_battery_killer_summary_on_attention)
        findPreference("misc.source").setOnPreferenceClickListener {
            (activity as MainActivity).launchUrl("https://github.com/Mygod/DHCPv6-Client-Android".toUri())
            true
        }
        findPreference("misc.donate").setOnPreferenceClickListener {
            EBegFragment().show(fragmentManager, "ebeg_fragment")
            true
        }
    }

    override fun onResume() {
        super.onResume()
        val context = requireContext()
        batteryKiller.isChecked = Build.VERSION.SDK_INT < 23 ||
                context.getSystemService<PowerManager>()?.isIgnoringBatteryOptimizations(context.packageName) == false
    }
}
