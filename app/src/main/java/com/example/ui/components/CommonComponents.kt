package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.domain.model.ApprovalRequest
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun CommandCard(
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
fun StatusPill(
    label: String,
    color: Color,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TierBadge(tier: Int, modifier: Modifier = Modifier) {
    val (label, color) = when (tier) {
        1 -> "المستوى 1 • تلقائي" to EmeraldSuccess
        2 -> "المستوى 2 • موافقة" to AmberWarning
        else -> "المستوى 3 • صريحة مستقلة" to CrimsonError
    }
    StatusPill(
        label = label,
        color = color,
        icon = if (tier == 3) Icons.Rounded.Lock else null,
        modifier = modifier
    )
}

@Composable
fun AnimatedProgressRing(
    progressPercent: Int,
    modifier: Modifier = Modifier,
    size: Dp = 88.dp,
    strokeWidth: Dp = 9.dp
) {
    val targetSweep = (progressPercent.coerceIn(0, 100) / 100f) * 360f
    val animatedSweep by animateFloatAsState(
        targetValue = targetSweep,
        animationSpec = spring(),
        label = "ring_sweep"
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(primaryColor, tertiaryColor, primaryColor)),
                startAngle = -90f,
                sweepAngle = animatedSweep,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
        Text(
            text = "${progressPercent.coerceIn(0, 100)}%",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun MetricsBarChart(
    items: List<Pair<String, Pair<Float, Color>>>,
    modifier: Modifier = Modifier
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { (label, pair) ->
            val (ratio, barColor) = pair
            val clamped = ratio.coerceIn(0f, 1f)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(92.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                ) {
                    drawRoundRect(
                        color = trackColor,
                        cornerRadius = CornerRadius(8f, 8f),
                        size = size
                    )
                    if (clamped > 0f) {
                        drawRoundRect(
                            color = barColor,
                            cornerRadius = CornerRadius(8f, 8f),
                            size = Size(width = size.width * clamped, height = size.height),
                            topLeft = Offset.Zero
                        )
                    }
                }
                Text(
                    text = "${(clamped * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = barColor
                )
            }
        }
    }
}

@Composable
fun LtrCodeBox(
    code: String,
    modifier: Modifier = Modifier,
    highlightDiff: Boolean = false
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF070B14),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                val lines = code.ifBlank { "// لا يوجد محتوى" }.lines()
                lines.forEach { line ->
                    val lineColor = when {
                        highlightDiff && line.startsWith("+") -> EmeraldSuccess
                        highlightDiff && line.startsWith("-") -> CrimsonError
                        highlightDiff && line.startsWith("@@") -> ElectricCyan
                        else -> Color(0xFFE2E8F0)
                    }
                    Text(
                        text = line,
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.bodySmall,
                        color = lineColor
                    )
                }
            }
        }
    }
}

@Composable
fun SetupRequiredBanner(
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    CommandCard(
        modifier = modifier,
        borderColor = AmberWarning.copy(alpha = 0.55f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = null,
                tint = AmberWarning
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Button(
            onClick = onAction,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        ) {
            Text(actionLabel)
        }
    }
}

@Composable
fun ApprovalGateCard(
    approval: ApprovalRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tierInt = approval.tier.toInt()
    val borderColor = if (tierInt == 3) CrimsonError else AmberWarning
    CommandCard(
        modifier = modifier,
        borderColor = borderColor.copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TierBadge(tier = tierInt)
            StatusPill(
                label = when (approval.status) {
                    "approved" -> "تمت الموافقة"
                    "executed" -> "تم التنفيذ"
                    "execution_reported" -> "أُبلغ عن التنفيذ (غير متحقق)"
                    "rejected" -> "مرفوض"
                    else -> "بانتظار الموافقة"
                },
                color = when (approval.status) {
                    "approved", "executed" -> EmeraldSuccess
                    "execution_reported" -> MaterialTheme.colorScheme.primary
                    "rejected" -> CrimsonError
                    else -> AmberWarning
                },
                icon = when (approval.status) {
                    "approved", "executed" -> Icons.Rounded.CheckCircle
                    "execution_reported" -> Icons.Rounded.Info
                    "rejected" -> Icons.Rounded.ErrorOutline
                    else -> Icons.Rounded.Schedule
                }
            )
        }
        Text(
            text = approval.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = approval.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (approval.targetResource.isNotBlank()) {
            Text(
                text = "الهدف: ${approval.targetResource}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (approval.payloadPreview.isNotBlank()) {
            LtrCodeBox(
                code = approval.payloadPreview,
                highlightDiff = true
            )
        }
        if (approval.status == "pending") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onApprove,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("approve_button_${approval.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (tierInt == 3) CrimsonError else EmeraldSuccess,
                        contentColor = Color.White
                    )
                ) {
                    Text(if (tierInt == 3) "موافقة صريحة وتنفيذ" else "اعتماد وتنفيذ")
                }
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("reject_button_${approval.id}")
                ) {
                    Text("رفض")
                }
            }
        }
    }
}

@Composable
fun QuotaFailoverBanner(
    exhaustedLabel: String,
    switchedToLabel: String?,
    reason: String,
    onDismiss: () -> Unit,
    onManageAi: () -> Unit,
    modifier: Modifier = Modifier
) {
    CommandCard(
        modifier = modifier.testTag("quota_failover_banner"),
        borderColor = AmberWarning
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = AmberWarning)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "تنبيه استهلاك API: انتهت حصة «$exhaustedLabel»",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AmberWarning
                )
                val desc = if (!switchedToLabel.isNullOrBlank()) {
                    "تم التحويل التلقائي إلى «$switchedToLabel» لمتابعة العمل دون انقطاع. ($reason)"
                } else {
                    "لا توجد مفاتيح بديلة متاحة حالياً. أضف مفتاحاً جديداً أو أعد التفعيل. ($reason)"
                }
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onManageAi,
                modifier = Modifier.weight(1f)
            ) {
                Text("إدارة مفاتيح AI")
            }
            Button(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) {
                Text("فهمت")
            }
        }
    }
}
