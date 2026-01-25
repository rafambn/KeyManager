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
import org.jetbrains.compose.resources.stringResource
import keymanager.composeapp.generated.resources.Res
import keymanager.composeapp.generated.resources.validity_valid
import keymanager.composeapp.generated.resources.validity_expiring
import keymanager.composeapp.generated.resources.validity_expired
import keymanager.composeapp.generated.resources.validity_unknown

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

    val validLabel = stringResource(Res.string.validity_valid)
    val expiringLabel = stringResource(Res.string.validity_expiring)
    val expiredLabel = stringResource(Res.string.validity_expired)
    val unknownLabel = stringResource(Res.string.validity_unknown)

    val (backgroundColor, contentColor, icon, label) = when (status) {
        ValidityStatus.VALID -> Quadruple(
            Color(0xFF4CAF50).copy(alpha = 0.15f),
            Color(0xFF2E7D32),
            Icons.Default.Check,
            validLabel
        )
        ValidityStatus.EXPIRING_SOON -> Quadruple(
            Color(0xFFFF9800).copy(alpha = 0.15f),
            Color(0xFFE65100),
            Icons.Default.Warning,
            expiringLabel
        )
        ValidityStatus.EXPIRED -> Quadruple(
            Color(0xFFF44336).copy(alpha = 0.15f),
            Color(0xFFC62828),
            Icons.Default.Close,
            expiredLabel
        )
        ValidityStatus.UNKNOWN -> Quadruple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            null,
            unknownLabel
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
        // Try multiple date formats to handle different locales
        val expiryDate = parseDateFlexible(validUntil)
        val today = LocalDate.now()
        val daysUntilExpiry = ChronoUnit.DAYS.between(today, expiryDate)

        when {
            daysUntilExpiry < 0 -> ValidityStatus.EXPIRED
            daysUntilExpiry <= 30 -> ValidityStatus.EXPIRING_SOON
            else -> ValidityStatus.VALID
        }
    } catch (e: Exception) {
        ValidityStatus.UNKNOWN
    }
}

private fun parseDateFlexible(dateString: String): LocalDate {
    val trimmed = dateString.trim()

    // Try parsing with English locale first
    try {
        val formatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy", java.util.Locale.ENGLISH)
        val dateTime = ZonedDateTime.parse(trimmed, formatter)
        return dateTime.toLocalDate()
    } catch (e: Exception) {
        // Continue to next format
    }

    // Try parsing with Portuguese locale
    try {
        val formatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy", java.util.Locale("pt", "BR"))
        val dateTime = ZonedDateTime.parse(trimmed, formatter)
        return dateTime.toLocalDate()
    } catch (e: Exception) {
        // Continue to next format
    }

    // Try ISO format (last 4 digits for year, then back to extract the full date)
    try {
        val parts = trimmed.split(" ")
        if (parts.size >= 3) {
            val month = when (parts[1].lowercase()) {
                "jan" -> "01"
                "fev", "feb" -> "02"
                "mar" -> "03"
                "abr", "apr" -> "04"
                "mai", "may" -> "05"
                "jun" -> "06"
                "jul" -> "07"
                "ago", "aug" -> "08"
                "set", "sep" -> "09"
                "out", "oct" -> "10"
                "nov" -> "11"
                "dez", "dec" -> "12"
                else -> throw IllegalArgumentException("Unknown month: ${parts[1]}")
            }
            val day = parts[2].padStart(2, '0')
            val year = parts.last()
            val isoDate = "$year-$month-$day"
            return LocalDate.parse(isoDate)
        }
    } catch (e: Exception) {
        // Continue to next format
    }

    // Final fallback: try to extract just the date portion
    try {
        val isoMatch = Regex("""(\d{4})-(\d{2})-(\d{2})""").find(trimmed)
        if (isoMatch != null) {
            return LocalDate.parse(isoMatch.value)
        }
    } catch (e: Exception) {
        // Fall through to throw
    }

    throw IllegalArgumentException("Could not parse date: $dateString")
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
