package com.smsfinance.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ── Shimmer animation brush ───────────────────────────────────────────────────

@Composable
fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateX by transition.animateFloat(
        initialValue   = -600f,
        targetValue    = 1400f,
        animationSpec  = infiniteRepeatable(
            animation  = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerX"
    )
    return Brush.linearGradient(
        colors = listOf(
            Color(0xFF2A3347),
            Color(0xFF3A4560),
            Color(0xFF2A3347),
        ),
        start = Offset(translateX, 0f),
        end   = Offset(translateX + 600f, 300f)
    )
}

// ── Base shimmer box ──────────────────────────────────────────────────────────

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp,
    brush: Brush = rememberShimmerBrush()
) {
    Box(
        modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush)
    )
}

// ── Dashboard skeleton ────────────────────────────────────────────────────────

@Composable
fun DashboardSkeleton() {
    val brush = rememberShimmerBrush()

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(4.dp))

        // Top bar skeleton
        Row(
            Modifier.fillMaxWidth(),
            Arrangement.SpaceBetween,
            Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ShimmerBox(Modifier.size(9.dp), cornerRadius = 50.dp, brush = brush)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ShimmerBox(Modifier.width(110.dp).height(14.dp), brush = brush)
                    ShimmerBox(Modifier.width(80.dp).height(10.dp), brush = brush)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(4) { ShimmerBox(Modifier.size(36.dp), cornerRadius = 50.dp, brush = brush) }
            }
        }

        // Greeting card skeleton
        ShimmerBox(Modifier.fillMaxWidth().height(72.dp), cornerRadius = 16.dp, brush = brush)

        // Hero balance card skeleton
        ShimmerBox(Modifier.fillMaxWidth().height(140.dp), cornerRadius = 20.dp, brush = brush)

        // Quick actions skeleton
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ShimmerBox(Modifier.weight(1f).height(52.dp), cornerRadius = 14.dp, brush = brush)
            ShimmerBox(Modifier.weight(1f).height(52.dp), cornerRadius = 14.dp, brush = brush)
        }

        Spacer(Modifier.height(4.dp))

        // Recent activity header skeleton
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ShimmerBox(Modifier.width(130.dp).height(16.dp), brush = brush)
                ShimmerBox(Modifier.width(90.dp).height(10.dp), brush = brush)
            }
            ShimmerBox(Modifier.width(60.dp).height(28.dp), cornerRadius = 8.dp, brush = brush)
        }

        // Transaction row skeletons
        repeat(3) {
            TransactionRowSkeleton(brush)
        }
    }
}

@Composable
fun TransactionRowSkeleton(brush: Brush = rememberShimmerBrush()) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1C2537))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Icon circle
        ShimmerBox(Modifier.size(38.dp), cornerRadius = 50.dp, brush = brush)
        // Source + description
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ShimmerBox(Modifier.width(110.dp).height(13.dp), brush = brush)
            ShimmerBox(Modifier.width(80.dp).height(10.dp), brush = brush)
        }
        // Amount
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ShimmerBox(Modifier.width(70.dp).height(13.dp), brush = brush)
            ShimmerBox(Modifier.width(50.dp).height(10.dp), brush = brush)
        }
    }
}

// ── Resume overlay — shown briefly when returning from another app ────────────

@Composable
fun ResumeLoadingOverlay(visible: Boolean) {
    if (!visible) return
    val brush = rememberShimmerBrush()
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1520).copy(alpha = 0.85f))
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ShimmerBox(Modifier.fillMaxWidth().height(140.dp), cornerRadius = 20.dp, brush = brush)
            repeat(2) { TransactionRowSkeleton(brush) }
        }
    }
}