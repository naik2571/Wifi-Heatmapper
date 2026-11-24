package com.wifi.heatmap.model

import android.graphics.Color

data class WifiDataPoint(
    val x: Float,
    val y: Float,
    val rssi: Int, // Signal strength in dBm (-30 to -95)
    val linkSpeed: Int, // Mbps
    val frequency: Int, // MHz (e.g. 2412, 5180)
    val timestamp: Long = System.currentTimeMillis()
) {
    fun getSignalColor(): Int {
        return when {
            rssi >= -55 -> Color.parseColor("#10B981") // Excellent (Emerald Green)
            rssi >= -65 -> Color.parseColor("#06B6D4") // Good (Cyan)
            rssi >= -75 -> Color.parseColor("#F59E0B") // Fair (Amber Warning)
            else -> Color.parseColor("#EF4444")        // Poor / Dead-Zone (Red)
        }
    }

    fun getQualityDescription(): String {
        return when {
            rssi >= -55 -> "EXCELLENT"
            rssi >= -65 -> "GOOD"
            rssi >= -75 -> "FAIR"
            else -> "DEAD-ZONE"
        }
    }
}
