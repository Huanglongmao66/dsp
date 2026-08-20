package com.dsp.immersiveshortvideo.ui.components

import androidx.compose.foundation.Image
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dsp.immersiveshortvideo.R
import com.dsp.immersiveshortvideo.model.ShortVideo

/**
 * 左侧底部信息面板：作者名、描述文案、话题标签、音乐信息
 */
@Composable
fun BottomInfoPanel(
    video: ShortVideo,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, colorResource(R.color.overlay_shadow))
                )
            )
            .padding(start = 16.dp, end = 90.dp, bottom = 28.dp, top = 80.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ===== 1. @作者名 =====
        Text(
            text = "@${video.authorName}",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        // ===== 2. 描述文案 + 话题标签 =====
        Text(
            text = buildAnnotatedString {
                append(video.description)
                // 把话题标签染成蓝色高亮
                val text = video.description
                video.tags.forEach { tag ->
                    val keyword = "#$tag"
                    val idx = text.indexOf(keyword)
                    if (idx >= 0) {
                        addStyle(
                            style = SpanStyle(color = colorResource(R.color.comment_blue)),
                            start = idx,
                            end = idx + keyword.length
                        )
                    }
                }
            },
            color = colorResource(R.color.white_90),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            maxLines = 3
        )

        // ===== 3. 音乐信息（带音符图标） =====
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 2.dp)
        ) {
            Text("♪", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(6.dp))
            Text(
                text = "${video.musicAuthor} - ${video.musicName}",
                color = colorResource(R.color.white_70),
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    }
}

/**
 * 顶部Tab栏：关注 / 推荐 / 附近 （沉浸式半透明效果）
 */
@Composable
fun TopTabBar(
    modifier: Modifier = Modifier
) {
    val tabs = listOf("关注", "推荐", "同城")
    val selectedIndex = 1 // 默认"推荐"
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = index == selectedIndex
            Text(
                text = tab,
                color = if (isSelected) Color.White else colorResource(R.color.white_50),
                fontSize = if (isSelected) 18.sp else 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.padding(horizontal = 14.dp)
            )
            if (index < tabs.size - 1) {
                Box(
                    modifier = Modifier
                        .height(12.dp)
                        .width(1.dp)
                        .background(colorResource(R.color.white_30))
                )
            }
        }
    }
}

/**
 * 视频底部进度条（贴底，不遮挡视频内容）
 */
@Composable
fun VideoProgressBar(
    progressMs: Long,
    durationMs: Long,
    modifier: Modifier = Modifier
) {
    val percent = if (durationMs <= 0L) 0f else (progressMs.toFloat() / durationMs).coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(color = Color(0x4000000))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(percent)
                .height(3.dp)
                .background(color = Color(0xFF25F4EE))
        )
    }
}
