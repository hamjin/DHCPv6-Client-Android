package be.mygod.dhcpv6client

import android.annotation.TargetApi
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.lifecycle.Observer

@TargetApi(24)
class Dhcp6cTileService : TileService(), Observer<Boolean> {
    private val tile by lazy { Icon.createWithResource(application, R.drawable.ic_image_looks_6) }

    override fun onStartListening() {
        super.onStartListening()
        Dhcp6cService.enabled.observeForever(this)
        Dhcp6cService.enabled.value = Dhcp6cService.enabled.value
    }

    override fun onChanged(enabled: Boolean) {
        qsTile?.run {
            state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            icon = tile
            label = getString(R.string.app_name)
            updateTile()
        }
    }

    override fun onStopListening() {
        Dhcp6cService.enabled.removeObserver(this)
        super.onStopListening()
    }

    override fun onClick() {
        Dhcp6cService.enabled.value = !Dhcp6cService.enabled.value!!
    }
}
