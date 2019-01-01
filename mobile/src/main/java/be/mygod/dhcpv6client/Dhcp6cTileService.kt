package be.mygod.dhcpv6client

import android.annotation.TargetApi
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.lifecycle.Observer

@TargetApi(24)
class Dhcp6cTileService : TileService() {
    private val tile by lazy { Icon.createWithResource(application, R.drawable.ic_image_looks_6) }
    private val enabledObserver = Observer<Boolean> { updateTile(enabled = it) }
    private val addressObserver = Observer<Lazy<String>> { updateTile(addresses = it.value) }

    private fun updateTile(enabled: Boolean = Dhcp6cService.enabled.value!!,
                           addresses: String? = Dhcp6cDaemon.addresses.value?.value) = qsTile?.run {
        state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        icon = tile
        label = if (addresses.isNullOrBlank()) getString(R.string.app_name) else addresses
        updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        Dhcp6cService.enabled.value = Dhcp6cService.enabled.value
        Dhcp6cService.enabled.observeForever(enabledObserver)
        Dhcp6cDaemon.addresses.observeForever(addressObserver)
    }

    override fun onStopListening() {
        Dhcp6cDaemon.addresses.removeObserver(addressObserver)
        Dhcp6cService.enabled.removeObserver(enabledObserver)
        super.onStopListening()
    }

    override fun onClick() {
        Dhcp6cService.enabled.value = !Dhcp6cService.enabled.value!!
    }
}
