package com.rafambn.keymanager.ui.components.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun LockIndicator(
    isLocked: Boolean,
    modifier: Modifier = Modifier,
    lockedColor: Color = MaterialTheme.colorScheme.outline,
    unlockedColor: Color = MaterialTheme.colorScheme.primary
) {
    Icon(
        imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
        contentDescription = if (isLocked) "Locked" else "Unlocked",
        tint = if (isLocked) lockedColor else unlockedColor,
        modifier = modifier
    )
}
