package be.mygod.dhcpv6client

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import be.mygod.dhcpv6client.App.Companion.app
import be.mygod.dhcpv6client.preference.SharedPreferenceDataStore

class MainPreferenceFragment : PreferenceFragmentCompat() {
    private lateinit var batteryKiller: SwitchPreference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = SharedPreferenceDataStore(app.pref)
        addPreferencesFromResource(R.xml.pref_main)
        batteryKiller = findPreference("service.batteryKiller") as SwitchPreference
        batteryKiller.setOnPreferenceChangeListener { _, _ ->
            if (Build.VERSION.SDK_INT >= 23)
                requireContext().startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            false
        }
        findPreference("misc.source").setOnPreferenceClickListener {
            app.launchUrl("https://github.com/Mygod/DHCPv6-Client-Android".toUri())
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
        batteryKiller.isChecked = Build.VERSION.SDK_INT >= 23 &&
                context.getSystemService<PowerManager>()?.isIgnoringBatteryOptimizations(context.packageName) == false
    }
}
