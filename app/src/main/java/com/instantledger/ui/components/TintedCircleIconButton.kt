package com.instantledger.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp

/**
 * Circular action button with tinted "glass" background and border.
 * Uses MaterialTheme.colorScheme.primaryContainer with 0.15f alpha for fill
 * and a 0.5.dp border with higher alpha for a glass look.
 * Triggers LongPress haptic on click. Capped at 48.dp for smaller screens.
 */
@Composable
fun TintedCircleIconButton(
    onClick: () -> Unit,
    imageVector: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    iconTint: Color? = null
) {
    val view = LocalView.current
    val containerColor = MaterialTheme.colorScheme.primaryContainer
    IconButton(
        onClick = {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            onClick()
        },
        modifier = modifier
            .sizeIn(maxWidth = 48.dp, maxHeight = 48.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(containerColor.copy(alpha = 0.15f))
                .border(
                    width = 0.5.dp,
                    color = containerColor.copy(alpha = 0.45f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = iconTint ?: MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
