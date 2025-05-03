package com.example.gatekeep.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
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

class ParticleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val particles = mutableListOf<Particle>()
    private val particlePaint = Paint().apply { 
        isAntiAlias = true
    }
    private val maxParticles = 25
    private var animationJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private val tealColor = context.getColor(R.color.teal_500)
    private var isInitialized = false
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        // Initialize particles when we know our size
        if (!isInitialized && w > 0 && h > 0) {
            isInitialized = true
            initializeParticles()
        }
    }
    
    private fun initializeParticles() {
        // Clear any existing particles
        particles.clear()
        
        // Initialize particles
        for (i in 0 until maxParticles) {
            addParticle()
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
                    updateParticles()
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
    
    private fun updateParticles() {
        if (width <= 0 || height <= 0) return
        
        particles.forEach { particle ->
            // Update position
            particle.x += particle.speedX
            particle.y += particle.speedY
            
            // Apply slight acceleration towards center
            val centerX = width / 2f
            val centerY = height / 2f
            particle.speedX += (centerX - particle.x) * 0.0001f * particle.gravity
            particle.speedY += (centerY - particle.y) * 0.0001f * particle.gravity
            
            // Apply decay
            particle.alpha = (particle.alpha - 0.005f).coerceAtLeast(0.1f)
            
            // Reset if out of bounds or too transparent
            if (particle.x < 0 || particle.x > width || 
                particle.y < 0 || particle.y > height ||
                particle.alpha <= 0.1f) {
                resetParticle(particle)
            }
        }
    }
    
    private fun addParticle() {
        if (width <= 0 || height <= 0) return
        particles.add(createRandomParticle())
    }
    
    private fun createRandomParticle(): Particle {
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = (Math.min(width, height) * 0.3f).toFloat()
        val angle = Random.nextFloat() * Math.PI * 2
        
        return Particle(
            x = centerX + (radius * cos(angle)).toFloat(),
            y = centerY + (radius * sin(angle)).toFloat(),
            radius = Random.nextFloat() * 4f + 1f,
            speedX = (Random.nextFloat() - 0.5f) * 2f,
            speedY = (Random.nextFloat() - 0.5f) * 2f,
            alpha = Random.nextFloat() * 0.5f + 0.5f,
            color = tealColor,
            gravity = Random.nextFloat() * 0.5f + 0.5f
        )
    }
    
    private fun resetParticle(particle: Particle) {
        if (width <= 0 || height <= 0) return
        val newParticle = createRandomParticle()
        particle.x = newParticle.x
        particle.y = newParticle.y
        particle.speedX = newParticle.speedX
        particle.speedY = newParticle.speedY
        particle.alpha = newParticle.alpha
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (!isInitialized || width <= 0 || height <= 0) return
        
        particles.forEach { particle ->
            particlePaint.color = particle.color
            particlePaint.alpha = (particle.alpha * 255).toInt()
            canvas.drawCircle(particle.x, particle.y, particle.radius, particlePaint)
        }
    }
    
    data class Particle(
        var x: Float,
        var y: Float,
        val radius: Float,
        var speedX: Float,
        var speedY: Float,
        var alpha: Float,
        val color: Int,
        val gravity: Float
    )
} 