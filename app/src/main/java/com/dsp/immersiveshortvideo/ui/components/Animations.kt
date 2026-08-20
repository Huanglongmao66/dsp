package com.dsp.immersiveshortvideo.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dsp.immersiveshortvideo.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val heartColor = Color(0xFFFE2C55)

/**
 * 双击点赞：屏幕上弹出的飘心动画（位置由双击坐标决定）
 * 动画总时长约 950ms，与 VM 的 HEART_ANIM_DURATION_MS 对齐
 */
@Composable
fun FloatingHeartAnimation(
    anchorX: Float,
    anchorY: Float,
    onAnimationEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(0.2f) }
    val alpha = remember { Animatable(0f) }
    val translateY = remember { Animatable(0f) }

    LaunchedEffect(anchorX, anchorY) {
        // 放大+淡入
        scale.animateTo(1.2f, animationSpec = tween(180, easing = FastOutSlowInEasing))
        alpha.animateTo(1f, animationSpec = tween(100))
        delay(80)
        scale.animateTo(1f, animationSpec = tween(100))
        delay(120)
        // 向上飘+淡出（并行执行，缩短总时长）
        launch { translateY.animateTo(-180f, animationSpec = tween(400)) }
        launch { alpha.animateTo(0f, animationSpec = tween(400)) }
        delay(420)
        onAnimationEnd()
    }

    Box(
        modifier = modifier
            .offset { IntOffset(anchorX.toInt() - 60.dp.roundToPx() / 2, anchorY.toInt() - 60.dp.roundToPx() / 2) }
            .alpha(alpha.value)
            .scale(scale.value)
            .offset { IntOffset(0, translateY.value.toInt()) }
            .size(60.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawHeart(heartColor, center = Offset(size.width / 2, size.height / 2), size = size.minDimension)
        }
    }
}

/** 绘制心形路径 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHeart(
    color: Color,
    center: Offset,
    size: Float
) {
    val path = Path().apply {
        val s = size / 2
        moveTo(center.x, center.y + s * 0.35f)
        cubicTo(
            center.x - s * 1.1f, center.y - s * 0.2f,
            center.x - s * 0.4f, center.y - s * 0.9f,
            center.x, center.y - s * 0.25f
        )
        cubicTo(
            center.x + s * 0.4f, center.y - s * 0.9f,
            center.x + s * 1.1f, center.y - s * 0.2f,
            center.x, center.y + s * 0.35f
        )
        close()
    }
    drawPath(path = path, color = color)
    drawPath(path = path, color = Color.White.copy(alpha = 0.4f), style = Stroke(width = size * 0.04f))
}

/**
 * 亮度/音量/快进快退的手势指示Toast
 */
@Composable
fun GestureIndicatorOverlay(
    state: GestureIndicatorState,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = state.visible,
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(200)),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .background(Color(0x88000000), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(state.icon, fontSize = 36.sp, color = Color.White)
                    Text(state.text, fontSize = 14.sp, color = Color.White)
                    if (state.progress != null) {
                        Box(
                            modifier = Modifier
                                .size(width = 70.dp, height = 4.dp)
                                .background(Color(0x55FFFFFF), RoundedCornerShape(2.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(
                                        width = (70 * state.progress.coerceIn(0f, 1f)).dp,
                                        height = 4.dp
                                    )
                                    .background(Color.White, RoundedCornerShape(2.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 手势指示弹窗状态 */
data class GestureIndicatorState(
    val visible: Boolean = false,
    val icon: String = "",
    val text: String = "",
    val progress: Float? = null
)
