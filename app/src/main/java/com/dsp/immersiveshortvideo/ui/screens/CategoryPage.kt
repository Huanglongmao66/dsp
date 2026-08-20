package com.dsp.immersiveshortvideo.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dsp.immersiveshortvideo.data.ApiConfig
import com.dsp.immersiveshortvideo.data.ApiConfigManager

/**
 * 分类选择页面
 * 显示默认推荐 + API配置列表 + 自定义接口入口
 * 选择分类时传递ApiConfig给调用方
 */
@Composable
fun CategoryPage(
    onDismiss: () -> Unit,
    onCategorySelected: (ApiConfig?) -> Unit,
    onOpenApiSettings: () -> Unit,
    currentCategoryName: String = "默认推荐"
) {
    val context = LocalContext.current
    val configManager = remember { ApiConfigManager(context) }
    var configs by remember { mutableStateOf(configManager.getEnabledConfigs()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "分类",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Text(
                text = "默认推荐",
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        if (currentCategoryName == "默认推荐") Color(0xFF25F4EE) else Color(0xFF333333)
                    )
                    .clickable { onCategorySelected(null) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🎬 默认推荐",
                    color = if (currentCategoryName == "默认推荐") Color.Black else Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "分类视频",
                color = Color(0xCCFFFFFF),
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(configs) { config ->
                    CategoryConfigItem(
                        config = config,
                        isSelected = config.name == currentCategoryName,
                        onClick = { onCategorySelected(config) }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenApiSettings() }
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2A2A2A))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "自定义接口",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "设置",
                        tint = Color(0xFF25F4EE),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/**
 * API配置分类项组件
 * 圆形emoji图标 + 名称
 */
@Composable
private fun CategoryConfigItem(
    config: ApiConfig,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .size(width = 72.dp, height = 90.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(
                    if (isSelected) Color(0xFF25F4EE) else Color(0xFF333333)
                ),
            contentAlignment = Alignment.Center
        ) {
            IconForName(
                iconName = config.icon,
                tint = if (isSelected) Color.Black else Color.White,
                size = 24.dp
            )
        }
        Text(
            text = config.name,
            color = if (isSelected) Color(0xFF25F4EE) else Color(0xCCFFFFFF),
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
