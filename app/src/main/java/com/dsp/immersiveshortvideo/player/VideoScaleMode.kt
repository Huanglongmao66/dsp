package com.dsp.immersiveshortvideo.player

import androidx.media3.ui.AspectRatioFrameLayout

/**
 * 视频画面缩放模式（用户可选项）
 * 内部映射到 Media3 AspectRatioFrameLayout.RESIZE_MODE_*，
 * 其中 [ORIGINAL] 需要额外调整 Surface 尺寸实现。
 */
enum class VideoScaleMode(
    val displayName: String,
    val resizeMode: Int,
    val description: String
) {
    DEFAULT(
        displayName = "默认",
        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
        description = "裁剪铺满全屏（推荐）"
    ),
    FIT(
        displayName = "等比",
        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT,
        description = "完整显示，黑边补齐"
    ),
    FILL(
        displayName = "铺满",
        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
        description = "裁剪内容，全屏铺满"
    ),
    CROP(
        displayName = "裁剪",
        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
        description = "居中裁剪，不留黑边"
    ),
    STRETCH(
        displayName = "拉伸",
        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL,
        description = "强制拉伸铺满，画面会变形"
    ),
    ORIGINAL(
        displayName = "原始尺寸",
        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT,
        description = "按视频原始像素尺寸显示"
    ),
    ADAPTIVE(
        displayName = "自适应",
        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT,
        description = "自动按比例适配屏幕"
    ),
    PAD(
        displayName = "填充黑边",
        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT,
        description = "完整显示，不足部分填充黑边"
    );

    companion object {
        fun fromName(name: String?, fallback: VideoScaleMode = DEFAULT): VideoScaleMode {
            if (name == null) return fallback
            return runCatching { valueOf(name) }.getOrDefault(fallback)
        }
    }
}
