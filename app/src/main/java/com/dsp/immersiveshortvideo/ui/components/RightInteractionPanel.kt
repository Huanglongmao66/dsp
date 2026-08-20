package com.dsp.immersiveshortvideo.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.dsp.immersiveshortvideo.R
import kotlinx.coroutines.delay

/**
 * 右侧互动按钮面板
 * 3个按钮：爱心、下载、展开
 * （眼睛按钮已独立在外，不受此面板控制）
 */
@Composable
fun RightInteractionPanel(
    isLiked: Boolean,
    isDownloaded: Boolean,
    isHideAllUi: Boolean,
    onLikeClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onEyeClick: () -> Unit,
    onExpandClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // ===== 1. 爱心 =====
        ActionButton(
            icon = {
                Icon(
                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "点赞",
                    tint = if (isLiked) colorResource(R.color.like_red) else Color.White,
                    modifier = Modifier.size(32.dp)
                )
            },
            onClick = onLikeClick
        )

        // ===== 2. 下载 =====
        ActionButton(
            icon = {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = "下载",
                    tint = if (isDownloaded) Color(0xFF25F4EE) else Color.White,
                    modifier = Modifier.size(30.dp)
                )
            },
            onClick = onDownloadClick
        )

        // ===== 3. 展开菜单 =====
        ActionButton(
            icon = {
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = "更多设置",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            },
            onClick = onExpandClick
        )
    }
}

@Composable
private fun ActionButton(
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    LaunchedEffect(scale) {
        if (scale != 1f) {
            delay(120)
            scale = 1f
        }
    }
    Box(
        modifier = Modifier
            .size(48.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { scale = 1.25f; onClick() },
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}
