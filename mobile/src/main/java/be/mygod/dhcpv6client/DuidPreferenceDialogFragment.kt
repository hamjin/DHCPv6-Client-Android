package be.mygod.dhcpv6client

import androidx.appcompat.app.AlertDialog
import be.mygod.dhcpv6client.widget.SmartSnackbar
import com.takisoft.preferencex.EditTextPreferenceDialogFragmentCompat

class DuidPreferenceDialogFragment : EditTextPreferenceDialogFragmentCompat() {
    override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
        super.onPrepareDialogBuilder(builder)
        builder.setNeutralButton(R.string.settings_service_duid_generate) { _, _ ->
            Dhcp6cManager.generateDuid()
            SmartSnackbar.make(R.string.settings_service_duid_success).show()
        }
    }
}
