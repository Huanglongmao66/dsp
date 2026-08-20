package com.dsp.immersiveshortvideo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dsp.immersiveshortvideo.player.VideoScaleMode

/**
 * 视频设置半屏菜单（BottomSheet风格）
 * 结构：
 * 标题 -> 顶部快捷图标（4个：复制/下载/纯净/缓存）-> 连播开关
 * -> 速度（SettingRow点击弹出单选选择框）-> 缓存管理
 * -> 画面模式（SettingRow点击弹出单选选择框，仅4项）
 */
@Composable
fun VideoSettingsSheet(
    onDismiss: () -> Unit,
    isAutoPlayNext: Boolean,
    currentSpeed: Float,
    speedOptions: List<Float>,
    scaleMode: VideoScaleMode,
    scaleModeOptions: List<VideoScaleMode>,
    isHideAllUi: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    cacheSizeText: String,
    onAutoPlayToggle: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onScaleModeChange: (VideoScaleMode) -> Unit,
    onDownload: () -> Unit,
    onClearScreen: () -> Unit,
    onCopyUrl: () -> Unit,
    onClearCache: () -> Unit
) {
    var showSpeedPicker by remember { mutableStateOf(false) }
    var showScalePicker by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x66000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(Color(0xFF1A1A1A))
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ===== 标题 =====
            Text(
                text = "设置",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            // ===== 顶部 4 个快捷图标（复制/下载/纯净/缓存）=====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuickIconButton(
                    iconVector = Icons.Filled.ContentCopy,
                    label = "复制",
                    tint = Color.White,
                    onClick = onCopyUrl
                )
                QuickIconButton(
                    iconVector = Icons.Filled.Download,
                    label = if (isDownloaded) "已下" else if (isDownloading) "下中" else "下载",
                    tint = if (isDownloaded) Color(0xFF25F4EE) else Color.White,
                    enabled = !isDownloading,
                    onClick = onDownload
                )
                QuickIconButton(
                    iconVector = if (isHideAllUi) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    label = "纯净",
                    tint = Color(0xFF25F4EE),
                    onClick = onClearScreen
                )
                QuickIconButton(
                    iconVector = Icons.Filled.Delete,
                    label = "缓存",
                    tint = Color(0xFFFF5252),
                    onClick = onClearCache
                )
            }

            // ===== 1. 连播（右侧标准 Switch 开关）=====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x15FFFFFF))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onAutoPlayToggle
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "连播",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isAutoPlayNext) "自动播放下一条" else "手动切换下一条",
                    color = Color(0x99FFFFFF),
                    fontSize = 12.sp
                )
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = isAutoPlayNext,
                    onCheckedChange = { onAutoPlayToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = Color(0xFF25F4EE),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0x33FFFFFF)
                    )
                )
            }

            // ===== 2. 速度（点击整行弹出单选选择框）=====
            SettingRow(
                icon = {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFF25F4EE),
                        modifier = Modifier.size(24.dp)
                    )
                },
                title = "速度",
                subtitle = "${currentSpeed}x（0.25~3）",
                onClick = { showSpeedPicker = true }
            )

            // ===== 3. 缓存管理 =====
            SettingRow(
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(24.dp)
                    )
                },
                title = "缓存管理",
                subtitle = "当前缓存: $cacheSizeText",
                onClick = onClearCache
            )

            // ===== 4. 画面模式（点击整行弹出单选选择框，仅4项）=====
            SettingRow(
                icon = {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF25F4EE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "▢",
                            color = Color.Black,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                title = "画面模式",
                subtitle = "${scaleMode.displayName}（${scaleMode.description}）",
                onClick = { showScalePicker = true }
            )
        }
    }

    // ===== 速度选择框（单选）=====
    if (showSpeedPicker) {
        SingleChoiceDialog(
            title = "选择播放速度",
            options = speedOptions,
            selected = currentSpeed,
            displayText = { "${it}x" },
            onConfirm = {
                onSpeedChange(it)
                showSpeedPicker = false
            },
            onDismiss = { showSpeedPicker = false }
        )
    }

    // ===== 画面模式选择框（单选）=====
    if (showScalePicker) {
        SingleChoiceDialog(
            title = "选择画面模式",
            options = scaleModeOptions,
            selected = scaleMode,
            displayText = { it.displayName + " — " + it.description },
            onConfirm = {
                onScaleModeChange(it)
                showScalePicker = false
            },
            onDismiss = { showScalePicker = false }
        )
    }
}

/**
 * 通用单选对话框：标题 + 列表（每个左边 RadioButton + 文字）+ 取消按钮
 */
@Composable
private fun <T> SingleChoiceDialog(
    title: String,
    options: List<T>,
    selected: T,
    displayText: (T) -> String,
    onConfirm: (T) -> Unit,
    onDismiss: () -> Unit
) {
    var localSelected by remember(selected) { mutableStateOf(selected) }

    AlertDialog(
        containerColor = Color(0xFF202020),
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                options.forEach { option ->
                    val isSelected = option == localSelected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { localSelected = option }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { localSelected = option },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFF25F4EE),
                                unselectedColor = Color(0x77FFFFFF)
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = displayText(option),
                            color = if (isSelected) Color(0xFF25F4EE) else Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(localSelected) }) {
                Text("确定", color = Color(0xFF25F4EE), fontSize = 14.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color(0x99FFFFFF), fontSize = 14.sp)
            }
        }
    )
}

@Composable
private fun QuickIconButton(
    iconVector: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x15FFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = label,
            color = if (enabled) Color.White else Color(0x55FFFFFF),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SettingRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {},
    enabled: Boolean = true,
    showArrow: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x15FFFFFF))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (enabled) Color.White else Color(0x66FFFFFF),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = if (enabled) Color(0x99FFFFFF) else Color(0x55FFFFFF),
                fontSize = 12.sp,
                maxLines = 1
            )
        }
        if (showArrow) {
            Text(
                text = "›",
                color = Color(0x66FFFFFF),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Suppress("unused")
@Composable
private fun RowScope.ScaleModeChip(
    mode: VideoScaleMode,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) Color(0xFF25F4EE) else Color(0x22FFFFFF)
    )
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = mode.displayName,
            color = if (selected) Color(0xFF000000) else Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
