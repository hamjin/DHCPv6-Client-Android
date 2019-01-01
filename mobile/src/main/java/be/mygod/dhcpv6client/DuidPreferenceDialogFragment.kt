package be.mygod.dhcpv6client

import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.preference.EditTextPreference
import androidx.preference.EditTextPreferenceDialogFragmentCompat
import be.mygod.dhcpv6client.widget.SmartSnackbar

class DuidPreferenceDialogFragment : EditTextPreferenceDialogFragmentCompat() {
    fun setKey(key: String) {
        arguments = bundleOf(Pair(ARG_KEY, key))
    }

    override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
        super.onPrepareDialogBuilder(builder)
        builder.setNeutralButton(R.string.settings_service_duid_generate) { _, _ ->
            Dhcp6cManager.generateDuid()
            (preference as EditTextPreference).text = Dhcp6cManager.duidString
            SmartSnackbar.make(R.string.settings_service_duid_success).show()
        }
    }
}
