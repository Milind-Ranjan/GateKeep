package com.example.gatekeep.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.util.AttributeSet
import android.view.View
import com.example.gatekeep.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class OrbitalView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val orbits = mutableListOf<Orbit>()
    private val orbitPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    private val dotPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    
    private var animationJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    
    private val pathMeasure = PathMeasure()
    private val point = floatArrayOf(0f, 0f)
    private val tan = floatArrayOf(0f, 0f)
    private var isInitialized = false
    
    private val tealColors = listOf(
        context.getColor(R.color.teal_200),
        context.getColor(R.color.teal_300),
        context.getColor(R.color.teal_400),
        context.getColor(R.color.teal_500)
    )
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        // Initialize orbits when we know our size
        if (!isInitialized && w > 0 && h > 0) {
            isInitialized = true
            initializeOrbits()
        }
    }
    
    private fun initializeOrbits() {
        // Clear any existing orbits
        orbits.clear()
        
        // Create orbits
        for (i in 0 until 4) {
            orbits.add(createOrbit(i))
        }
    }
    
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAnimation()
    }
    
    override fun onDetachedFromWindow() {
        stopAnimation()
        super.onDetachedFromWindow()
    }
    
    private fun startAnimation() {
        animationJob?.cancel()
        animationJob = scope.launch {
            while (isActive) {
                if (isInitialized) {
                    orbits.forEach { orbit ->
                        orbit.progress = (orbit.progress + orbit.speed) % 1f
                    }
                    invalidate()
                }
                delay(16) // ~60fps
            }
        }
    }
    
    private fun stopAnimation() {
        animationJob?.cancel()
        animationJob = null
    }
    
    private fun createOrbit(index: Int): Orbit {
        val rotation = Random.nextFloat() * 360f
        val radiusMultiplier = 0.6f + (index * 0.25f)
        val speed = 0.002f - (index * 0.0004f)
        val color = tealColors[index % tealColors.size]
        val alpha = 0.6f - (index * 0.1f)
        
        return Orbit(
            rotation = rotation,
            radiusMultiplier = radiusMultiplier,
            speed = speed,
            color = color,
            alpha = alpha,
            progress = Random.nextFloat()
        )
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (!isInitialized || width <= 0 || height <= 0) return
        
        val centerX = width / 2f
        val centerY = height / 2f
        val baseRadius = Math.min(width, height) * 0.18f
        
        orbits.forEach { orbit ->
            val path = Path()
            val radius = baseRadius * orbit.radiusMultiplier
            
            // Create elliptical orbit path
            val left = centerX - radius
            val top = centerY - radius
            val right = centerX + radius
            val bottom = centerY + radius
            path.addOval(left, top, right, bottom, Path.Direction.CW)
            
            // Rotate the path
            val matrix = android.graphics.Matrix()
            matrix.setRotate(orbit.rotation, centerX, centerY)
            path.transform(matrix)
            
            // Draw orbit path
            orbitPaint.color = orbit.color
            orbitPaint.alpha = (orbit.alpha * 70).toInt()
            canvas.drawPath(path, orbitPaint)
            
            // Draw dot on path
            pathMeasure.setPath(path, false)
            pathMeasure.getPosTan(pathMeasure.length * orbit.progress, point, tan)
            
            dotPaint.color = orbit.color
            dotPaint.alpha = (orbit.alpha * 255).toInt()
            canvas.drawCircle(point[0], point[1], 6f, dotPaint)
        }
    }
    
    data class Orbit(
        val rotation: Float,
        val radiusMultiplier: Float,
        val speed: Float,
        val color: Int,
        val alpha: Float,
        var progress: Float
    )
} 