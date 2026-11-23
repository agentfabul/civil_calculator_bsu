package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Architecture
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.calc.CalcFmt
import com.example.domain.calc.CalcStatus
import com.example.domain.calc.CalcStep
import com.example.domain.calc.CalculationNature
import com.example.domain.calc.IssueSeverity
import com.example.domain.calc.SOFTWARE_VERSION
import com.example.domain.calc.UnitSystem
import com.example.domain.calc.ValidationIssue
import com.example.ui.theme.MonoEquationStyle
import com.example.ui.theme.StatusEstimateAccent
import com.example.ui.theme.StatusEstimateContainer
import com.example.ui.theme.StatusEstimateText
import com.example.ui.theme.StatusFailAccent
import com.example.ui.theme.StatusFailContainer
import com.example.ui.theme.StatusFailText
import com.example.ui.theme.StatusPassAccent
import com.example.ui.theme.StatusPassContainer
import com.example.ui.theme.StatusPassText
import com.example.ui.theme.StatusWarnAccent
import com.example.ui.theme.StatusWarnContainer
import com.example.ui.theme.StatusWarnText

@Composable
fun CalcStatusBadge(
    status: CalcStatus,
    modifier: Modifier = Modifier
) {
    val (bg, fg, icon) = when (status) {
        CalcStatus.PASS -> Triple(StatusPassContainer, StatusPassText, Icons.Outlined.CheckCircle)
        CalcStatus.WARNING -> Triple(StatusWarnContainer, StatusWarnText, Icons.Outlined.WarningAmber)
        CalcStatus.FAIL -> Triple(StatusFailContainer, StatusFailText, Icons.Outlined.ErrorOutline)
        CalcStatus.ESTIMATE -> Triple(StatusEstimateContainer, StatusEstimateText, Icons.Outlined.Architecture)
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.testTag("status_badge_${status.code}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = status.label,
                tint = fg,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = status.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = fg
            )
        }
    }
}

@Composable
fun NatureBadge(
    nature: CalculationNature,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Text(
            text = nature.label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun UtilizationMeter(
    ratio: Double,
    label: String,
    modifier: Modifier = Modifier
) {
    val clamped = ratio.toFloat().coerceIn(0f, 1.25f)
    val progress = (clamped / 1f).coerceIn(0f, 1f)
    val accentColor = when {
        ratio > 1.0 -> StatusFailAccent
        ratio > 0.85 -> StatusWarnAccent
        else -> StatusPassAccent
    }
    val statusWord = when {
        ratio > 1.0 -> "OVERSTRESSED (> 1.00)"
        ratio > 0.85 -> "NEAR CAPACITY (> 0.85)"
        else -> "WITHIN CAPACITY (≤ 1.00)"
    }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${CalcFmt.num(ratio, 3)} ($statusWord)",
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = accentColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun ValidationIssueBanner(
    issue: ValidationIssue,
    modifier: Modifier = Modifier
) {
    val (bg, fg, accent, icon) = when (issue.severity) {
        IssueSeverity.ERROR -> Quad(StatusFailContainer, StatusFailText, StatusFailAccent, Icons.Outlined.ErrorOutline)
        IssueSeverity.WARNING -> Quad(StatusWarnContainer, StatusWarnText, StatusWarnAccent, Icons.Outlined.WarningAmber)
        IssueSeverity.INFO -> Quad(StatusEstimateContainer, StatusEstimateText, StatusEstimateAccent, Icons.Outlined.Info)
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = issue.severity.name,
                tint = fg,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "[${issue.severity.name}] ${issue.title}",
                    style = MaterialTheme.typography.labelLarge,
                    color = fg,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = issue.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = fg
                )
            }
        }
    }
}

@Composable
fun EquationStepCard(
    step: CalcStep,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Text(
                        text = "${step.stepNumber}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Formula: ${step.equation}",
                        style = MonoEquationStyle,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (step.variables.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Where: ${step.variables.joinToString(" | ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Substituted: ${step.substituted}",
                        style = MonoEquationStyle,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Result:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = step.resultFormatted,
                    style = MonoEquationStyle,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            step.rawPrecisionNote?.let { note ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = note,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun UnitSystemSelector(
    selected: UnitSystem,
    onSelect: (UnitSystem) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.testTag("unit_system_selector")) {
        UnitSystem.entries.forEachIndexed { index, unit ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = UnitSystem.entries.size),
                onClick = { onSelect(unit) },
                selected = selected == unit,
                modifier = Modifier.testTag("unit_option_${unit.code}")
            ) {
                Text(unit.label, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun EngineeringSafeguardNotice(
    scopeItems: List<String>,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.VerifiedUser,
                    contentDescription = "Engineering Safeguard",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Scope, Limitations & Professional Review",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            scopeItems.forEach { item ->
                Text(
                    text = "• $item",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Decision Support Only (CivilSpec $SOFTWARE_VERSION): A passing check does not prove a complete design is safe. Qualified Professional Engineer (PE/SE) review is required before construction or permitting.",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
