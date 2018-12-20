package be.mygod.dhcpv6client

import android.annotation.TargetApi
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

@TargetApi(24)
class Dhcp6cTileService : TileService() {
    private val tile by lazy { Icon.createWithResource(application, R.drawable.ic_image_looks_6) }

    override fun onStartListening() {
        super.onStartListening()
        Dhcp6cService.enabledChanged[this] = { enabled ->
            qsTile?.run {
                state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                icon = tile
                label = getString(R.string.app_name)
                updateTile()
            }
        }
    }

    override fun onStopListening() {
        Dhcp6cService.enabledChanged -= this
        super.onStopListening()
    }

    override fun onClick() {
        Dhcp6cService.enabled = !Dhcp6cService.enabled
    }
}
