package com.wifi.heatmap.pdr

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pedestrian Dead Reckoning (PDR) Tracking Engine.
 * Accurately calculates indoor walking displacement and azimuth orientation without GPS.
 */
class PdrTracker(context: Context) : SensorEventListener {

    interface PdrCallback {
        fun onStepDetected(x: Float, y: Float, headingDeg: Float, totalSteps: Int)
        fun onHeadingChanged(headingDeg: Float)
    }

    var callback: PdrCallback? = null

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    var currentX = 0f
        private set
    var currentY = 0f
        private set
    var currentHeadingDeg = 0f
        private set
    var totalSteps = 0
        private set

    private val stepLengthMeters = 0.72f // Average human indoor step length

    // Fallback step detection using accelerometer magnitude peaks
    private var lastAccMagnitude = 9.8f
    private var lastPeakTime = 0L
    private val minStepIntervalMs = 280L

    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    fun startTracking() {
        if (stepDetector != null) {
            sensorManager.registerListener(this, stepDetector, SensorManager.SENSOR_DELAY_FASTEST)
        } else {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }
        sensorManager.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_UI)
    }

    fun stopTracking() {
        sensorManager.unregisterListener(this)
    }

    fun reset() {
        currentX = 0f
        currentY = 0f
        totalSteps = 0
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientation)
                // Azimuth in degrees [0, 360)
                var azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                if (azimuth < 0) azimuth += 360f
                currentHeadingDeg = azimuth
                callback?.onHeadingChanged(currentHeadingDeg)
            }
            Sensor.TYPE_STEP_DETECTOR -> {
                registerStep()
            }
            Sensor.TYPE_ACCELEROMETER -> {
                // Fallback step counting if device has no hardware step detector
                if (stepDetector == null) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    val mag = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                    val delta = mag - lastAccMagnitude
                    lastAccMagnitude = mag

                    val now = System.currentTimeMillis()
                    if (delta > 1.6f && (now - lastPeakTime) > minStepIntervalMs) {
                        lastPeakTime = now
                        registerStep()
                    }
                }
            }
        }
    }

    private fun registerStep() {
        totalSteps++
        val rad = Math.toRadians(currentHeadingDeg.toDouble())
        // Coordinate projection: X is East/West, Y is North/South (upwards is negative Y in Canvas)
        currentX += (stepLengthMeters * sin(rad)).toFloat()
        currentY -= (stepLengthMeters * cos(rad)).toFloat()

        callback?.onStepDetected(currentX, currentY, currentHeadingDeg, totalSteps)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
