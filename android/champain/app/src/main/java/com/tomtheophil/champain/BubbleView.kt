package com.tomtheophil.champain

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.MotionEvent
import android.view.View
import kotlin.math.min
import kotlin.random.Random

/**
 * A custom view that displays animated champagne bubbles.
 * Bubbles can be created through touch events or programmatically based on frequency.
 */
class BubbleView(context: Context) : View(context) {

    data class Bubble(
        var x: Float,
        var baseX: Float,  // Store original X position for wobble
        var y: Float,
        var size: Float,
        var startTime: Long,  // Track when the bubble started moving
        var speed: Float,     // Vertical speed in pixels per second
        var alpha: Int = BubbleConfig.ALPHA,
        var birthTime: Long = System.currentTimeMillis(),
        var lifetime: Long = Random.nextLong(BubbleConfig.MIN_LIFETIME, BubbleConfig.MAX_LIFETIME),
        var isPopping: Boolean = false
    )

    private object BubbleConfig {
        const val MAX_PER_SECOND = 300f
        const val MIN_SIZE = 60f
        const val MAX_SIZE = 180f
        const val MIN_SPEED = 600f  // Pixels per second
        const val MAX_SPEED = 1200f
        const val ALPHA = 140
        const val MIN_LIFETIME = 500L
        const val MAX_LIFETIME = 3000L
        const val POP_DURATION = 300L
        const val GRADIENT_CENTER_ALPHA = 0.1f
        const val TAP_RADIUS = 100f
        
        // Animation configuration
        const val INITIAL_SPEED_FACTOR = 0.3f
        const val SPEED_RAMP_DURATION = 500L
        const val WOBBLE_FREQUENCY = 1f  // Complete wobble cycles per second
        const val WOBBLE_AMPLITUDE = 10f // Maximum pixels to move left/right
    }

    private val bubbles = mutableListOf<Bubble>()
    private val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var currentFrequency = 0f
    private var lastDrawTime = 0L

    init {
        isClickable = true
        isFocusable = true
        setBackgroundColor(Color.TRANSPARENT)
    }

    fun updateFrequency(frequency: Float) {
        currentFrequency = frequency
        invalidate()
    }

    private fun createBubble(x: Float? = null, y: Float? = null): Bubble {
        val size = Random.nextFloat() * (BubbleConfig.MAX_SIZE - BubbleConfig.MIN_SIZE) + BubbleConfig.MIN_SIZE
        val position = calculateBubblePosition(x, y, size)
        val speed = Random.nextFloat() * (BubbleConfig.MAX_SPEED - BubbleConfig.MIN_SPEED) + BubbleConfig.MIN_SPEED
        val currentTime = System.currentTimeMillis()  // Get current time for start time
        
        return Bubble(
            x = position.first,
            baseX = position.first,
            y = position.second,
            size = size,
            startTime = currentTime,
            speed = speed
        )
    }

    private fun calculateBubblePosition(tapX: Float?, tapY: Float?, size: Float): Pair<Float, Float> {
        val screenWidth = width.toFloat()
        val screenHeight = height.toFloat()
        
        val x = tapX?.let {
            (it + (Random.nextFloat() - 0.5f) * BubbleConfig.TAP_RADIUS * 2)
                .coerceIn(size/2, screenWidth - size/2)
        } ?: (Random.nextFloat() * (screenWidth - size))

        val y = tapY?.let {
            (it + (Random.nextFloat() - 0.5f) * BubbleConfig.TAP_RADIUS * 2)
                .coerceIn(size/2, screenHeight - size/2)
        } ?: (Random.nextFloat() * (screenHeight - size))

        return Pair(x, y)
    }

    private fun startPopAnimation(bubble: Bubble) {
        if (bubble.isPopping) return
        bubble.isPopping = true

        AnimatorSet().apply {
            val expand = createExpandAnimator(bubble)
            val fade = createFadeAnimator(bubble)
            playTogether(expand, fade)
            addListener(createPopAnimationListener(bubble))
            start()
        }
    }

    private fun createExpandAnimator(bubble: Bubble) = ValueAnimator.ofFloat(bubble.size, bubble.size * 1.5f).apply {
        duration = BubbleConfig.POP_DURATION
        addUpdateListener { bubble.size = it.animatedValue as Float }
    }

    private fun createFadeAnimator(bubble: Bubble) = ValueAnimator.ofInt(bubble.alpha, 0).apply {
        duration = BubbleConfig.POP_DURATION
        addUpdateListener { bubble.alpha = it.animatedValue as Int }
    }

    private fun createPopAnimationListener(bubble: Bubble) = object : android.animation.Animator.AnimatorListener {
        override fun onAnimationStart(animation: android.animation.Animator) {}
        override fun onAnimationEnd(animation: android.animation.Animator) {
            bubbles.remove(bubble)
            invalidate()
        }
        override fun onAnimationCancel(animation: android.animation.Animator) {}
        override fun onAnimationRepeat(animation: android.animation.Animator) {}
    }

    private fun updateBubblePositions(currentTime: Long) {
        bubbles.forEach { bubble ->
            if (!bubble.isPopping) {
                // Calculate elapsed time in seconds
                val elapsedTime = (currentTime - bubble.startTime) / 1000f
                
                // Update vertical position (linear upward movement)
                bubble.y = bubble.y - bubble.speed * (currentTime - lastDrawTime) / 1000f
                
                // Calculate wobble offset
                val wobblePhase = elapsedTime * BubbleConfig.WOBBLE_FREQUENCY * 2 * Math.PI
                val wobbleOffset = (Math.sin(wobblePhase) * BubbleConfig.WOBBLE_AMPLITUDE).toFloat()
                
                // Apply wobble to x position
                bubble.x = bubble.baseX + wobbleOffset
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val currentTime = System.currentTimeMillis()
        
        if (lastDrawTime > 0) {
            handleBubbleCreation(currentTime)
            updateBubblePositions(currentTime)
            handleBubbleLifetimes(currentTime)
        }

        // Remove bubbles that have moved off screen
        bubbles.removeAll { bubble -> 
            bubble.y + bubble.size < 0 && !bubble.isPopping 
        }

        drawBubbles(canvas)
        
        if (bubbles.isNotEmpty() || currentFrequency > 0) {
            invalidate()
        }

        lastDrawTime = currentTime
    }

    private fun handleBubbleCreation(currentTime: Long) {
        val deltaTime = (currentTime - lastDrawTime) / 1000f
        val bubblesPerSecond = calculateBubblesPerSecond()
        val bubblesToCreate = (bubblesPerSecond * deltaTime).toInt()
        
        repeat(bubblesToCreate) {
            bubbles.add(createBubble())
        }
    }

    private fun calculateBubblesPerSecond() = remap(
        min(currentFrequency, UIConstants.MAX_FREQUENCY),
        0f,
        UIConstants.MAX_FREQUENCY,
        0f,
        BubbleConfig.MAX_PER_SECOND
    )

    private fun handleBubbleLifetimes(currentTime: Long) {
        // Create a copy of the list to avoid concurrent modification
        val bubblesToCheck = bubbles.toList()
        bubblesToCheck.forEach { bubble ->
            if (!bubble.isPopping && currentTime - bubble.birthTime >= bubble.lifetime) {
                startPopAnimation(bubble)
            }
        }
    }

    private fun drawBubbles(canvas: Canvas) {
        bubbles.forEach { bubble ->
            bubblePaint.shader = createBubbleGradient(bubble)
            canvas.drawCircle(bubble.x, bubble.y, bubble.size/2, bubblePaint)
        }
    }

    private fun createBubbleGradient(bubble: Bubble) = RadialGradient(
        bubble.x, bubble.y, bubble.size/2,
        intArrayOf(
            Color.argb((bubble.alpha * BubbleConfig.GRADIENT_CENTER_ALPHA).toInt(), 255, 255, 255),
            Color.argb((bubble.alpha * 0.2f).toInt(), 255, 255, 255),
            Color.argb(bubble.alpha, 255, 255, 255)
        ),
        floatArrayOf(0f, 0.7f, 1f),
        Shader.TileMode.CLAMP
    )

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Create bubbles first
                repeat(5) { 
                    val bubble = createBubble(event.x, event.y)
                    bubbles.add(bubble)
                }
                invalidate()  // Force redraw
                performClick()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                repeat(3) { 
                    val bubble = createBubble(event.x, event.y)
                    bubbles.add(bubble)
                }
                invalidate()  // Force redraw
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        bubbles.clear()
        lastDrawTime = 0L
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    companion object {
        private fun remap(value: Float, low1: Float, high1: Float, low2: Float, high2: Float): Float {
            return low2 + (value - low1) * (high2 - low2) / (high1 - low1)
        }
    }
} 