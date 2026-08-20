package com.dsp.immersiveshortvideo.ui.components

import android.util.TypedValue
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.dsp.immersiveshortvideo.model.ShortVideo
import com.dsp.immersiveshortvideo.player.VideoScaleMode
import kotlin.math.min

/**
 * 单页视频播放器组件
 * 2层结构：底层封面图 + 中层 PlayerView（9:16全屏）
 * 支持 8 种画面缩放模式（由 VideoScaleMode 决定）
 */
@Composable
fun VideoPlayer(
    video: ShortVideo,
    player: Player?,
    isActivePage: Boolean,
    scaleMode: VideoScaleMode,
    videoPixelSize: Pair<Int, Int>,
    modifier: Modifier = Modifier
) {
    var showCover by remember(video.id) { mutableStateOf(true) }
    val ctx = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // 底层：封面图（首帧前显示，视频开始渲染后隐藏）
        if (showCover) {
            AsyncImage(
                model = video.coverUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = when (scaleMode) {
                    VideoScaleMode.STRETCH -> androidx.compose.ui.layout.ContentScale.FillBounds
                    VideoScaleMode.FIT,
                    VideoScaleMode.ADAPTIVE,
                    VideoScaleMode.PAD -> androidx.compose.ui.layout.ContentScale.Fit
                    VideoScaleMode.ORIGINAL -> androidx.compose.ui.layout.ContentScale.None
                    else -> androidx.compose.ui.layout.ContentScale.Crop
                }
            )
        }

        // 中层：Media3 PlayerView
        if (player != null) {
            AndroidView(
                factory = { context ->
                    PlayerView(context).apply {
                        useController = false
                        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                        setPlayer(player)
                        player.addListener(object : Player.Listener {
                            override fun onRenderedFirstFrame() {
                                showCover = false
                            }
                        })
                    }
                },
                update = { playerView ->
                    if (playerView.player !== player) {
                        playerView.player = player
                    }
                    // 应用缩放模式（切换模式时直接改变 PlayerView 的 resizeMode 与内部 Surface 尺寸）
                    val (origW, origH) = videoPixelSize
                    applyScaleMode(ctx, playerView, scaleMode, origW, origH)
                },
                modifier = if (scaleMode == VideoScaleMode.ORIGINAL && orig(videoPixelSize) > 0) {
                    // Compose 外层也把 PlayerView 限制到原始像素对应的 dp（防止 PlayerView 本身撑满容器）
                    val (w, h) = videoPixelSize
                    val density = ctx.resources.displayMetrics.density
                    val maxW = (ctx.resources.displayMetrics.widthPixels / density).dp
                    val maxH = (ctx.resources.displayMetrics.heightPixels / density).dp
                    val dpW = min(w / density, maxW.value).dp
                    val dpH = min(h / density, maxH.value).dp
                    Modifier.size(width = dpW, height = dpH)
                } else {
                    Modifier.fillMaxSize()
                }
            )
        }
    }
}

private fun orig(p: Pair<Int, Int>): Int = p.first * p.second

/**
 * 根据缩放模式设置 PlayerView 的 resizeMode 和 Surface 尺寸。
 * [ORIGINAL]：通过修改 VideoSurfaceView/TextureView 的 LayoutParams，将其限制为原始像素尺寸。
 * 其余模式：直接使用 AspectRatioFrameLayout 原生 resizeMode。
 */
private fun applyScaleMode(
    ctx: android.content.Context,
    playerView: PlayerView,
    mode: VideoScaleMode,
    videoW: Int,
    videoH: Int
) {
    playerView.resizeMode = mode.resizeMode

    val metrics = ctx.resources.displayMetrics
    val surfaceView = playerView.videoSurfaceView
    if (surfaceView != null) {
        val parent = surfaceView.parent as? ViewGroup
        val params = surfaceView.layoutParams
        if (mode == VideoScaleMode.ORIGINAL && videoW > 0 && videoH > 0) {
            // 原始尺寸：限制不超过屏幕尺寸
            val maxPxW = metrics.widthPixels
            val maxPxH = metrics.heightPixels
            val scale = min(
                maxPxW.toFloat() / videoW.toFloat(),
                maxPxH.toFloat() / videoH.toFloat()
            ).coerceAtMost(1f)
            params.width = (videoW * scale).toInt().coerceAtLeast(1)
            params.height = (videoH * scale).toInt().coerceAtLeast(1)
        } else {
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            params.height = ViewGroup.LayoutParams.MATCH_PARENT
        }
        surfaceView.layoutParams = params
        parent?.requestLayout()
    }
    // 兼容 AspectRatioFrameLayout 层
    (playerView as? AspectRatioFrameLayout)?.let { _ ->
        // 已经由 resizeMode 负责，无需额外处理
    }
    playerView.requestLayout()
}

@Suppress("unused")
private fun dpToPx(ctx: android.content.Context, dp: Float): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, ctx.resources.displayMetrics).toInt()

