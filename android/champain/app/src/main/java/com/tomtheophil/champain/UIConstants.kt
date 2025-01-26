package com.tomtheophil.champain

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object UIConstants {
    const val MAX_FREQUENCY = 10f // Maximum expected movements per second

    // Champagne colors for app background
    val CHAMPAGNE_DARK = Color(0xFFFFB300)    // Deep amber gold
    val CHAMPAGNE_MEDIUM = Color(0xFFFFD54F)  // Bright gold
    val CHAMPAGNE_LIGHT = Color(0xFFFFE082)   // Light gold/yellow
    
    // UI colors using grayscale with transparency
    val UI_DARK = Color.Black.copy(alpha = 0.7f)        // Semi-transparent black
    val UI_MEDIUM = Color.Black.copy(alpha = 0.5f)      // More transparent black
    val UI_LIGHT = Color.White.copy(alpha = 0.2f)       // Slightly transparent white
    
    // Common shapes
    val SHAPE_LARGE = RoundedCornerShape(16.dp)
    val SHAPE_MEDIUM = RoundedCornerShape(12.dp)
    val SHAPE_SMALL = RoundedCornerShape(8.dp)
    
    // Common content colors
    val CONTENT_PRIMARY = Color.White
    val CONTENT_SECONDARY = Color.White.copy(alpha = 0.7f)
    val CONTENT_DISABLED = Color.White.copy(alpha = 0.5f)
} 