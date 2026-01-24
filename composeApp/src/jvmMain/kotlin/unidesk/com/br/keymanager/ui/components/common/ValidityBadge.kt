package unidesk.com.br.keymanager.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

enum class ValidityStatus {
    VALID,
    EXPIRING_SOON,
    EXPIRED,
    UNKNOWN
}

@Composable
fun ValidityBadge(
    validUntil: String?,
    modifier: Modifier = Modifier
) {
    val status = calculateValidityStatus(validUntil)

    val (backgroundColor, contentColor, icon, label) = when (status) {
        ValidityStatus.VALID -> Quadruple(
            Color(0xFF4CAF50).copy(alpha = 0.15f),
            Color(0xFF2E7D32),
            Icons.Default.Check,
            "Valid"
        )
        ValidityStatus.EXPIRING_SOON -> Quadruple(
            Color(0xFFFF9800).copy(alpha = 0.15f),
            Color(0xFFE65100),
            Icons.Default.Warning,
            "Expiring"
        )
        ValidityStatus.EXPIRED -> Quadruple(
            Color(0xFFF44336).copy(alpha = 0.15f),
            Color(0xFFC62828),
            Icons.Default.Close,
            "Expired"
        )
        ValidityStatus.UNKNOWN -> Quadruple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            null,
            "Unknown"
        )
    }

    Box(
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor
            )
        }
    }
}

private fun calculateValidityStatus(validUntil: String?): ValidityStatus {
    if (validUntil.isNullOrBlank()) return ValidityStatus.UNKNOWN

    return try {
        val formatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy")
        val dateTime = ZonedDateTime.parse(validUntil, formatter)
        val expiryDate = dateTime.toLocalDate()
        val today = LocalDate.now()
        val daysUntilExpiry = ChronoUnit.DAYS.between(today, expiryDate)

        when {
            daysUntilExpiry < 0 -> ValidityStatus.EXPIRED
            daysUntilExpiry <= 30 -> ValidityStatus.EXPIRING_SOON
            else -> ValidityStatus.VALID
        }
    } catch (e: Exception) {
        // Try alternative date format (ISO date only)
        try {
            val simpleFormatter = DateTimeFormatter.ISO_LOCAL_DATE
            val expiryDate = LocalDate.parse(validUntil.take(10), simpleFormatter)
            val today = LocalDate.now()
            val daysUntilExpiry = ChronoUnit.DAYS.between(today, expiryDate)

            when {
                daysUntilExpiry < 0 -> ValidityStatus.EXPIRED
                daysUntilExpiry <= 30 -> ValidityStatus.EXPIRING_SOON
                else -> ValidityStatus.VALID
            }
        } catch (e2: Exception) {
            ValidityStatus.UNKNOWN
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
