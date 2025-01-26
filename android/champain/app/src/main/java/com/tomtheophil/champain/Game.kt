package com.tomtheophil.champain

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.rememberPagerState

/**
 * Main game screen that displays bubbles and handles interactions
 */
@Composable
fun Game(
    frequency: Float,
    connectionManager: ConnectionManager,
    onPop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bubbleView by remember { mutableStateOf<BubbleView?>(null) }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(createChampagneGradient())
    ) {
        // Create and manage BubbleView lifecycle
        AndroidView(
            factory = { ctx ->
                BubbleView(ctx).also { view ->
                    bubbleView = view
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                view.updateFrequency(frequency)
            }
        )
    }

    // Handle pop events
    DisposableEffect(bubbleView) {
        val scope = CoroutineScope(Dispatchers.Main)
        val popHandler = PopHandler(connectionManager, onPop, scope)
        
        bubbleView?.setOnClickListener {
            popHandler.handlePop()
        }

        onDispose {
            bubbleView?.setOnClickListener(null)
        }
    }
}

/**
 * Handles the logic for bubble popping events
 */
private class PopHandler(
    private val connectionManager: ConnectionManager,
    private val onPop: () -> Unit,
    private val scope: CoroutineScope
) {
    fun handlePop() {
        onPop()
        scope.launch {
            connectionManager.sendPop()
        }
    }
}

/**
 * Creates the champagne-colored gradient background
 */
private fun createChampagneGradient() = Brush.verticalGradient(
    colors = listOf(
        UIConstants.CHAMPAGNE_LIGHT,
        UIConstants.CHAMPAGNE_MEDIUM,
        UIConstants.CHAMPAGNE_MEDIUM,
        UIConstants.CHAMPAGNE_DARK
    )
)

/**
 * Main content container for the game screen
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GameContent(
    selectedPage: Int,
    sensors: SensorComponents,
    onPop: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (selectedPage) {
        0 -> Game(
            frequency = sensors.shakeDetector.currentFrequency.floatValue,
            connectionManager = sensors.connectionManager,
            onPop = onPop,
            modifier = modifier
        )
        1 -> SettingsPage(
            shakeDetector = sensors.shakeDetector,
            orientationDetector = sensors.orientationDetector,
            connectionManager = sensors.connectionManager,
            pagerState = rememberPagerState(pageCount = { 3 }),
            modifier = modifier
        )
    }
} 