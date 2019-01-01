package be.mygod.dhcpv6client

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import be.mygod.dhcpv6client.App.Companion.app
import be.mygod.dhcpv6client.widget.SmartSnackbar

class MainPreferenceFragment : PreferenceFragmentCompat() {
    private var backgroundRestriction: SwitchPreference? = null
    private lateinit var duid: EditTextPreference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_main)
        val addresses = findPreference<Preference>("service.addresses")
        Dhcp6cDaemon.addresses.observe(this, Observer { addresses.summary = it.value })
        val backgroundRestriction = findPreference<SwitchPreference>("service.backgroundRestriction")
        this.backgroundRestriction = backgroundRestriction
        if (Build.VERSION.SDK_INT < 23 || app.backgroundUnavailable) {
            backgroundRestriction.parent!!.removePreference(backgroundRestriction)
            this.backgroundRestriction = null
        } else backgroundRestriction.setOnPreferenceChangeListener { _, _ ->
            val context = requireContext()
            if (!backgroundRestriction.isChecked || ContextCompat.checkSelfPermission(context, Manifest.permission
                            .REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) != PackageManager.PERMISSION_GRANTED) try {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } catch (e: ActivityNotFoundException) {
                e.printStackTrace()
                app.analytics.logEvent("battery_opt_settings", bundleOf(Pair("message", e.message)))
            } catch (e: SecurityException) {
                e.printStackTrace()
                app.analytics.logEvent("battery_opt_settings", bundleOf(Pair("message", e.message)))
            } else startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    .setData("package:${context.packageName}".toUri()))
            false
        }
        duid = findPreference("service.duid")
        Dhcp6cManager.ensureDuid()
        duid.setOnPreferenceChangeListener { _, newValue ->
            try {
                Dhcp6cManager.duidString = newValue as String
                SmartSnackbar.make(R.string.settings_service_duid_success).show()
                true
            } catch (e: Exception) {
                SmartSnackbar.make(e.localizedMessage).show()
                e.printStackTrace()
                false
            }
        }
        findPreference<Preference>("misc.source").setOnPreferenceClickListener {
            (activity as MainActivity).launchUrl("https://github.com/Mygod/DHCPv6-Client-Android".toUri())
            true
        }
        findPreference<Preference>("misc.donate").setOnPreferenceClickListener {
            EBegFragment().show(fragmentManager ?: return@setOnPreferenceClickListener false, "EBegFragment")
            true
        }
    }

    override fun onResume() {
        super.onResume()
        val backgroundRestriction = backgroundRestriction
        if (backgroundRestriction != null) {
            val context = requireContext()
            backgroundRestriction.isChecked = Build.VERSION.SDK_INT >= 26 && context.getSystemService<PowerManager>()
                    ?.isIgnoringBatteryOptimizations(context.packageName) == false
        }
        duid.text = Dhcp6cManager.duidString
    }

    override fun onDisplayPreferenceDialog(preference: Preference?) {
        if (preference === duid) DuidPreferenceDialogFragment().apply {
            setKey(duid.key)
            setTargetFragment(this@MainPreferenceFragment, 0)
        }.show(fragmentManager ?: return, duid.key) else super.onDisplayPreferenceDialog(preference)
    }
}
