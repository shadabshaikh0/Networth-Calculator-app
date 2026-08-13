package com.shadabshaikh.networth.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.shadabshaikh.networth.domain.DonutSeg
import com.shadabshaikh.networth.domain.TrendPoint
import com.shadabshaikh.networth.ui.theme.hexToColor

/** Asset-allocation donut: one arc per category, drawn clockwise from the top. */
@Composable
fun DonutChart(segs: List<DonutSeg>, modifier: Modifier = Modifier, trackColor: Color, strokeWidth: Dp = 22.dp) {
    Canvas(modifier) {
        val sw = strokeWidth.toPx()
        val topLeft = Offset(sw / 2, sw / 2)
        val arcSize = Size(size.width - sw, size.height - sw)
        drawArc(
            color = trackColor, startAngle = 0f, sweepAngle = 360f, useCenter = false,
            topLeft = topLeft, size = arcSize, style = Stroke(sw),
        )
        var start = -90f
        segs.forEach { seg ->
            val sweep = seg.fraction * 360f
            drawArc(
                color = hexToColor(seg.colorHex), startAngle = start, sweepAngle = sweep,
                useCenter = false, topLeft = topLeft, size = arcSize,
                style = Stroke(sw, cap = StrokeCap.Butt),
            )
            start += sweep
        }
    }
}

/** Net-worth trend: filled area + line + dots, values normalized to the box. */
@Composable
fun TrendLineChart(points: List<TrendPoint>, lineColor: Color, modifier: Modifier = Modifier) {
    if (points.isEmpty()) return
    Canvas(modifier) {
        val pad = 8.dp.toPx()
        val w = size.width
        val h = size.height
        val values = points.map { it.value }
        val min = values.min()
        val max = values.max()
        val span = (max - min).toFloat().coerceAtLeast(1f)
        val n = points.size
        val xs = points.indices.map { i -> if (n == 1) w / 2 else pad + i * (w - 2 * pad) / (n - 1) }
        val ys = points.map { h - pad - ((it.value - min).toFloat() / span) * (h - 2 * pad) }

        val line = Path().apply {
            moveTo(xs[0], ys[0])
            for (i in 1 until n) lineTo(xs[i], ys[i])
        }
        val area = Path().apply {
            addPath(line)
            lineTo(xs.last(), h)
            lineTo(xs.first(), h)
            close()
        }
        drawPath(area, lineColor.copy(alpha = 0.12f))
        drawPath(line, lineColor, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        xs.forEachIndexed { i, x ->
            drawCircle(lineColor, radius = (if (i == n - 1) 4.dp else 3.dp).toPx(), center = Offset(x, ys[i]))
        }
    }
}
