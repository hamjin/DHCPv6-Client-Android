package be.mygod.dhcpv6client

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.support.customtabs.CustomTabsIntent
import android.support.v14.preference.SwitchPreference
import android.support.v4.content.ContextCompat
import be.mygod.dhcpv6client.App.Companion.app
import be.mygod.dhcpv6client.preference.SharedPreferenceDataStore
import be.mygod.dhcpv6client.util.systemService
import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat

class MainPreferenceFragment : PreferenceFragmentCompat() {
    private val customTabsIntent by lazy {
        CustomTabsIntent.Builder()
                .setToolbarColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
                .build()
    }

    private lateinit var batteryKiller: SwitchPreference

    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = SharedPreferenceDataStore(app.pref)
        addPreferencesFromResource(R.xml.pref_main)
        batteryKiller = findPreference("service.batteryKiller") as SwitchPreference
        batteryKiller.setOnPreferenceChangeListener { _, _ ->
            if (Build.VERSION.SDK_INT >= 23)
                requireContext().startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            false
        }
        findPreference("misc.source").setOnPreferenceClickListener {
            customTabsIntent.launchUrl(requireActivity(), Uri.parse("https://github.com/Mygod/DHCPv6-Client-Android"))
            true
        }
    }

    override fun onResume() {
        super.onResume()
        val context = requireContext()
        batteryKiller.isChecked = Build.VERSION.SDK_INT >= 23 &&
                !context.systemService<PowerManager>().isIgnoringBatteryOptimizations(context.packageName)
    }
}
