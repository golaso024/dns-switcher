package com.salamander.dnsswitch

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class DnsTileService : TileService() {

    // Обновляем состояние кнопки, когда шторка открывается
    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    // При клике открываем наше Activity-меню
    override fun onClick() {
        super.onClick()

        val intent = Intent(this, DnsSelectorActivity::class.java).apply {
            // Флаги нужны, чтобы запустить Activity из фонового сервиса
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Начиная с Android 14 нужен PendingIntent
            startActivityAndCollapse(
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun updateTileState() {
        val currentMode = Settings.Global.getString(contentResolver, "private_dns_mode")
        val isActive = currentMode == "hostname"
        val currentDns = Settings.Global.getString(contentResolver, "private_dns_specifier") ?: ""

        qsTile?.apply {
            state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            // Если включен, пишем какой именно DNS, если нет — просто OFF
            label = if (isActive) currentDns.replace("dns.", "") else "DNS: OFF"
            updateTile()
        }
    }
}
