package xyz.larkzhh.lime.ui.qrscan.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.larkzhh.lime.ui.theme.LimePrimary

@Composable
fun ScanOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "scanBar",
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        // 计算取景框的位置与大小
        val boxSize = size.width * 0.65f
        val left = (size.width - boxSize) / 2f
        val top = (size.height - boxSize) / 2f
        val scanRect = Rect(left, top, left + boxSize, top + boxSize)

        // 绘制半透明遮罩与镂空效果
        val path = Path().apply {
            addRect(Rect(Offset.Zero, Size(size.width, size.height)))
            addRoundRect(RoundRect(scanRect, CornerRadius(16.dp.toPx())))
        }
        clipPath(path, clipOp = ClipOp.Difference) {
            drawRect(color = Color.Black.copy(alpha = 0.55f))
        }

        // 绘制四角标记
        val cornerLen = 28.dp.toPx()
        val cornerStroke = 3.dp.toPx()
        val corners = listOf(
            Offset(scanRect.left, scanRect.top + cornerLen) to Offset(scanRect.left, scanRect.top),
            Offset(scanRect.left, scanRect.top) to Offset(scanRect.left + cornerLen, scanRect.top),
            Offset(scanRect.right - cornerLen, scanRect.top) to Offset(scanRect.right, scanRect.top),
            Offset(scanRect.right, scanRect.top) to Offset(scanRect.right, scanRect.top + cornerLen),
            Offset(scanRect.left, scanRect.bottom - cornerLen) to Offset(scanRect.left, scanRect.bottom),
            Offset(scanRect.left, scanRect.bottom) to Offset(scanRect.left + cornerLen, scanRect.bottom),
            Offset(scanRect.right - cornerLen, scanRect.bottom) to Offset(scanRect.right, scanRect.bottom),
            Offset(scanRect.right, scanRect.bottom) to Offset(scanRect.right, scanRect.bottom - cornerLen),
        )
        corners.forEach { (start, end) ->
            drawLine(
                color = Color.White,
                start = start,
                end = end,
                strokeWidth = cornerStroke,
                cap = StrokeCap.Round,// 设置线段端点为圆角
            )
        }

        // 绘制扫描横杆
        val barY = scanRect.top + scanRect.height * scanY// 计算扫描杆y轴
        drawLine(
            color = LimePrimary.copy(alpha = 0.85f),
            start = Offset(scanRect.left + 8.dp.toPx(), barY),
            end = Offset(scanRect.right - 8.dp.toPx(), barY),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }

    // 提示文字
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "请将二维码对准扫码框中心",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = (0.65f * 50 + 32).dp),
        )
    }
}
