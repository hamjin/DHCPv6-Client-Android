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
import androidx.fragment.app.DialogFragment
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import be.mygod.dhcpv6client.App.Companion.app
import be.mygod.dhcpv6client.widget.SmartSnackbar
import com.crashlytics.android.Crashlytics
import com.takisoft.preferencex.PreferenceFragmentCompat

class MainPreferenceFragment : PreferenceFragmentCompat() {
    private var backgroundRestriction: SwitchPreference? = null
    private lateinit var duid: EditTextPreference

    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_main)
        val backgroundRestriction = findPreference("service.backgroundRestriction") as SwitchPreference
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
                Crashlytics.logException(e)
            } catch (e: SecurityException) {
                e.printStackTrace()
                Crashlytics.logException(e)
            } else startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    .setData("package:${context.packageName}".toUri()))
            false
        }
        duid = findPreference("service.duid") as EditTextPreference
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
        findPreference("misc.source").setOnPreferenceClickListener {
            (activity as MainActivity).launchUrl("https://github.com/Mygod/DHCPv6-Client-Android".toUri())
            true
        }
        findPreference("misc.donate").setOnPreferenceClickListener {
            EBegFragment().apply { setStyle(DialogFragment.STYLE_NO_TITLE, 0) }.show(fragmentManager, "EBegFragment")
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
        if (preference === duid) displayPreferenceDialog(DuidPreferenceDialogFragment(), duid.key)
        else super.onDisplayPreferenceDialog(preference)
    }
}
