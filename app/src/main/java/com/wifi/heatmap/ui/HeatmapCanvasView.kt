package com.wifi.heatmap.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import com.wifi.heatmap.model.WifiDataPoint
import kotlin.math.cos
import kotlin.math.sin

class HeatmapCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#15293B")
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 6f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val heatPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val userMarkerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#06B6D4")
        style = Paint.Style.FILL
    }

    private val userRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#06B6D4")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFFFF")
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
    }

    private val dataPoints = mutableListOf<WifiDataPoint>()
    private var currentX = 0f
    private var currentY = 0f
    private var currentHeading = 0f

    // 1 meter in real world = 35 pixels on screen
    private val scalePixelsPerMeter = 35f

    fun addDataPoint(point: WifiDataPoint) {
        dataPoints.add(point)
        currentX = point.x
        currentY = point.y
        postInvalidateOnAnimation()
    }

    fun updateCurrentHeading(heading: Float) {
        currentHeading = heading
        postInvalidateOnAnimation()
    }

    fun clearMap() {
        dataPoints.clear()
        currentX = 0f
        currentY = 0f
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val centerX = w / 2f
        val centerY = h / 2f

        // Center map on user's current walk path
        val offsetX = centerX - (currentX * scalePixelsPerMeter)
        val offsetY = centerY - (currentY * scalePixelsPerMeter)

        // 1. Draw floor plan metric grid (1m x 1m lines)
        val gridSize = scalePixelsPerMeter * 2f
        var gx = offsetX % gridSize
        while (gx < w) {
            canvas.drawLine(gx, 0f, gx, h, gridPaint)
            gx += gridSize
        }
        var gy = offsetY % gridSize
        while (gy < h) {
            canvas.drawLine(0f, gy, w, gy, gridPaint)
            gy += gridSize
        }

        // 2. Draw Heatmap Radials (Voronoi signal bloom around surveyed points)
        val heatRadius = scalePixelsPerMeter * 2.8f
        for (dp in dataPoints) {
            val px = offsetX + (dp.x * scalePixelsPerMeter)
            val py = offsetY + (dp.y * scalePixelsPerMeter)
            val color = dp.getSignalColor()

            val gradient = RadialGradient(
                px, py, heatRadius,
                intArrayOf(
                    Color.argb(120, Color.red(color), Color.green(color), Color.blue(color)),
                    Color.argb(40, Color.red(color), Color.green(color), Color.blue(color)),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.6f, 1f),
                Shader.TileMode.CLAMP
            )
            heatPaint.shader = gradient
            canvas.drawCircle(px, py, heatRadius, heatPaint)
        }

        // 3. Draw Connecting Walk Path
        if (dataPoints.size > 1) {
            for (i in 0 until dataPoints.size - 1) {
                val p1 = dataPoints[i]
                val p2 = dataPoints[i + 1]

                val x1 = offsetX + (p1.x * scalePixelsPerMeter)
                val y1 = offsetY + (p1.y * scalePixelsPerMeter)
                val x2 = offsetX + (p2.x * scalePixelsPerMeter)
                val y2 = offsetY + (p2.y * scalePixelsPerMeter)

                pathPaint.color = p2.getSignalColor()
                canvas.drawLine(x1, y1, x2, y2, pathPaint)
            }
        }

        // 4. Draw Surveyed Point Markers
        for (dp in dataPoints) {
            val px = offsetX + (dp.x * scalePixelsPerMeter)
            val py = offsetY + (dp.y * scalePixelsPerMeter)
            pointPaint.color = dp.getSignalColor()
            canvas.drawCircle(px, py, 9f, pointPaint)
        }

        // 5. Draw User Current Marker & Azimuth Heading Arrow
        val userPx = offsetX + (currentX * scalePixelsPerMeter)
        val userPy = offsetY + (currentY * scalePixelsPerMeter)

        canvas.drawCircle(userPx, userPy, 14f, userMarkerPaint)
        canvas.drawCircle(userPx, userPy, 22f, userRingPaint)

        // Heading directional needle
        val rad = Math.toRadians(currentHeading.toDouble())
        val needleLength = 32f
        val needleX = userPx + (needleLength * sin(rad)).toFloat()
        val needleY = userPy - (needleLength * cos(rad)).toFloat()
        canvas.drawLine(userPx, userPy, needleX, needleY, headingPaint)
    }
}
