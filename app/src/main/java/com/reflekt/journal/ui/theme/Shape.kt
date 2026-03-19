package com.reflekt.journal.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Section 9.3 — Shape Tokens
val ReflektShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),   // Chips, small badges
    small      = RoundedCornerShape(12.dp),  // Icon buttons, small cards
    medium     = RoundedCornerShape(16.dp),  // Input fields, entry cards, habit cards
    large      = RoundedCornerShape(22.dp),  // Main content cards, goal cards, heatmap
    extraLarge = RoundedCornerShape(28.dp),  // Bottom sheets, modal dialogs
)

// ShapeFull — FAB, primary CTA buttons, mood pills, streak badges
val ShapeFull = RoundedCornerShape(50.dp)
