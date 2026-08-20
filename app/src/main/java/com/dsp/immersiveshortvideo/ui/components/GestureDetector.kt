package com.dsp.immersiveshortvideo.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.coroutineScope
import kotlin.math.abs

/**
 * 统一手势检测
 *
 * 架构：
 * 1. pointerInput (Initial pass) 处理拖拽手势
 *    - 横向拖拽：快进/快退
 *    - 边缘竖拖：亮度/音量
 *    - 中央竖拖：交给 VerticalPager
 * 2. combinedClickable (Main pass) 处理点击手势
 *    - 单击：回调
 *    - 双击：点赞
 *    - 长按：临时加速
 *
 * 使用 rememberUpdatedState 持有回调，确保 lambda 始终是最新的
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.videoGestureDetector(
    screenSizePx: IntSize,
    edgeThresholdPx: Float = 120f,
    onSingleTap: () -> Unit = {},
    onDoubleTap: (x: Float, y: Float) -> Unit = { _, _ -> },
    onHorizontalDragEnd: (totalDx: Float) -> Unit = {},
    onVerticalDragEnd: (isLeftEdge: Boolean, totalDy: Float) -> Unit = { _, _ -> },
    onLongPress: (x: Float, y: Float) -> Unit = { _, _ -> },
    onLongPressEnd: () -> Unit = {},
): Modifier {
    var lastDownPos by remember { mutableStateOf(Offset.Zero) }
    var longPressFired by remember { mutableStateOf(false) }

    // 使用 rememberUpdatedState 确保 pointerInput 协程内的回调始终是最新的
    val currentOnSingleTap = rememberUpdatedState(onSingleTap)
    val currentOnDoubleTap = rememberUpdatedState(onDoubleTap)
    val currentOnHorizontalDragEnd = rememberUpdatedState(onHorizontalDragEnd)
    val currentOnVerticalDragEnd = rememberUpdatedState(onVerticalDragEnd)
    val currentOnLongPress = rememberUpdatedState(onLongPress)
    val currentOnLongPressEnd = rememberUpdatedState(onLongPressEnd)

    val touchSlop = LocalViewConfiguration.current.touchSlop

    val dragInput = Modifier.pointerInput(Unit) {
        coroutineScope {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                lastDownPos = down.position

                val isLeftEdgeArea = down.position.x < edgeThresholdPx
                val isRightEdgeArea = screenSizePx.width > 0 && down.position.x > screenSizePx.width - edgeThresholdPx
                val isEdgeArea = isLeftEdgeArea || isRightEdgeArea

                var totalDx = 0f
                var totalDy = 0f
                var dragDirectionDecided: Int? = null
                var hasMoved = false

                do {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull() ?: break

                    if (change.isConsumed) break

                    totalDx += change.position.x - change.previousPosition.x
                    totalDy += change.position.y - change.previousPosition.y

                    if (!hasMoved && (abs(totalDx) > touchSlop || abs(totalDy) > touchSlop)) {
                        hasMoved = true
                    }

                    if (dragDirectionDecided == null) {
                        when {
                            abs(totalDx) > touchSlop && abs(totalDx) > abs(totalDy) * 1.2f -> {
                                dragDirectionDecided = 0
                                hasMoved = true
                                change.consume()
                            }
                            abs(totalDy) > touchSlop && abs(totalDy) > abs(totalDx) * 1.2f -> {
                                if (isEdgeArea) {
                                    dragDirectionDecided = 1
                                    hasMoved = true
                                    change.consume()
                                } else {
                                    break
                                }
                            }
                        }
                    } else if (dragDirectionDecided != null) {
                        change.consume()
                    }
                } while (event.changes.any { it.pressed })

                if (longPressFired) {
                    longPressFired = false
                    currentOnLongPressEnd.value()
                }

                when {
                    dragDirectionDecided == 0 && abs(totalDx) > 50f ->
                        currentOnHorizontalDragEnd.value(totalDx)
                    dragDirectionDecided == 1 && abs(totalDy) > 40f ->
                        currentOnVerticalDragEnd.value(isLeftEdgeArea, totalDy)
                }
            }
        }
    }

    return this.then(dragInput).then(
        Modifier.combinedClickable(
            onClick = { currentOnSingleTap.value() },
            onDoubleClick = { currentOnDoubleTap.value(lastDownPos.x, lastDownPos.y) },
            onLongClick = {
                longPressFired = true
                currentOnLongPress.value(lastDownPos.x, lastDownPos.y)
            }
        )
    )
}
