package com.wifi.heatmap

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.wifi.heatmap.databinding.ActivityMainBinding
import com.wifi.heatmap.model.WifiDataPoint
import com.wifi.heatmap.pdr.PdrTracker

class MainActivity : AppCompatActivity(), PdrTracker.PdrCallback {

    companion object {
        const val TAG = "WifiHeatmapMainActivity"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var wifiManager: WifiManager
    private lateinit var pdrTracker: PdrTracker

    private var isSurveying = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var deadZoneCount = 0

    private val wifiPollRunnable = object : Runnable {
        override fun run() {
            updateWifiMetrics()
            mainHandler.postDelayed(this, 1000L)
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val locGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        if (locGranted) {
            startSurvey()
        } else {
            Toast.makeText(this, "Location permission is required to read Wi-Fi signal", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        pdrTracker = PdrTracker(this).apply {
            callback = this@MainActivity
        }

        setupListeners()
        updateWifiMetrics()
    }

    private fun setupListeners() {
        binding.btnMasterAction.setOnClickListener {
            if (isSurveying) {
                stopSurvey()
            } else {
                checkPermissionsAndStart()
            }
        }

        binding.btnResetMap.setOnClickListener {
            pdrTracker.reset()
            binding.heatmapCanvas.clearMap()
            deadZoneCount = 0
            binding.tvPdrStats.text = "0 steps (0.0 m)"
            binding.tvDeadZoneIcon.text = "🟢"
            binding.tvDeadZoneSummary.text = "Map cleared. Start a new walking survey."
            Toast.makeText(this, "Heatmap reset", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermissionsAndStart() {
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }

        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        } else {
            startSurvey()
        }
    }

    private fun startSurvey() {
        isSurveying = true
        pdrTracker.startTracking()
        updateUIState()

        // Capture initial sample at origin
        sampleCurrentPoint(pdrTracker.currentX, pdrTracker.currentY)
        Toast.makeText(this, "Survey started! Walk around room with phone.", Toast.LENGTH_SHORT).show()
    }

    private fun stopSurvey() {
        isSurveying = false
        pdrTracker.stopTracking()
        updateUIState()
        Toast.makeText(this, "Survey paused. Heatmap saved on screen.", Toast.LENGTH_SHORT).show()
    }

    private fun updateWifiMetrics() {
        val info = wifiManager.connectionInfo
        val rssi = info?.rssi ?: -99
        val linkSpeed = info?.linkSpeed ?: 0
        val freq = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) info?.frequency ?: 2412 else 2412
        val ssid = info?.ssid?.replace("\"", "") ?: "Not Connected"

        binding.tvConnectedSsid.text = "Connected: $ssid"
        binding.tvRssiValue.text = "$rssi dBm"
        binding.tvLinkSpeed.text = "Speed: $linkSpeed Mbps"

        val bandText = if (freq > 4900) "5.0 GHz (Ch ${(freq - 5000) / 5})" else "2.4 GHz (Ch ${(freq - 2407) / 5})"
        binding.tvFrequency.text = "Band: $bandText"

        val dummy = WifiDataPoint(0f, 0f, rssi, linkSpeed, freq)
        val color = dummy.getSignalColor()
        binding.tvRssiValue.setTextColor(color)
        binding.tvQualityLabel.text = "${dummy.getQualityDescription()} SIGNAL"
        binding.tvQualityLabel.setTextColor(color)
    }

    private fun sampleCurrentPoint(x: Float, y: Float) {
        val info = wifiManager.connectionInfo
        val rssi = info?.rssi ?: -99
        val linkSpeed = info?.linkSpeed ?: 0
        val freq = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) info?.frequency ?: 2412 else 2412

        val dp = WifiDataPoint(x, y, rssi, linkSpeed, freq)
        binding.heatmapCanvas.addDataPoint(dp)

        if (rssi < -75) {
            deadZoneCount++
            binding.tvDeadZoneIcon.text = "🔴"
            binding.tvDeadZoneSummary.text = "⚠️ $deadZoneCount Dead-Zone areas detected! Signal dropped below -75 dBm. Mesh repeater suggested here."
        }
    }

    override fun onStepDetected(x: Float, y: Float, headingDeg: Float, totalSteps: Int) {
        runOnUiThread {
            val dist = totalSteps * 0.72f
            binding.tvPdrStats.text = "$totalSteps steps (${String.format("%.1f", dist)} m)"
            if (isSurveying) {
                sampleCurrentPoint(x, y)
            }
        }
    }

    override fun onHeadingChanged(headingDeg: Float) {
        runOnUiThread {
            binding.heatmapCanvas.updateCurrentHeading(headingDeg)
        }
    }

    private fun updateUIState() {
        if (isSurveying) {
            binding.tvSurveyStatusBadge.text = "RECORDING"
            binding.tvSurveyStatusBadge.setTextColor(Color.parseColor("#10B981"))
            binding.tvSurveyStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#1510B981"))

            binding.btnMasterAction.text = "PAUSE SURVEY"
            binding.btnMasterAction.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F59E0B"))
            binding.btnMasterAction.setTextColor(Color.parseColor("#0A0F1D"))
        } else {
            binding.tvSurveyStatusBadge.text = "STANDBY"
            binding.tvSurveyStatusBadge.setTextColor(Color.parseColor("#06B6D4"))
            binding.tvSurveyStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#1506B6D4"))

            binding.btnMasterAction.text = "START HEATMAP SURVEY"
            binding.btnMasterAction.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#06B6D4"))
            binding.btnMasterAction.setTextColor(Color.parseColor("#0A0F1D"))
        }
    }

    override fun onResume() {
        super.onResume()
        mainHandler.post(wifiPollRunnable)
        if (isSurveying) {
            pdrTracker.startTracking()
        }
    }

    override fun onPause() {
        super.onPause()
        mainHandler.removeCallbacks(wifiPollRunnable)
        pdrTracker.stopTracking()
    }
}
