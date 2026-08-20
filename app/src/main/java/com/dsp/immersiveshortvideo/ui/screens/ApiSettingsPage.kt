package com.dsp.immersiveshortvideo.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.dsp.immersiveshortvideo.data.ApiIcons
import com.dsp.immersiveshortvideo.data.VideoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 自定义接口设置页面
 * 支持：添加/编辑/删除/刷新/复制接口
 * 刷新功能：检测接口可用性
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiSettingsPage(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val configManager = remember { ApiConfigManager(context) }
    var configs by remember { mutableStateOf(configManager.loadConfigs()) }
    var showAddSheet by remember { mutableStateOf(false) }
    var editingConfig by remember { mutableStateOf<ApiConfig?>(null) }
    val scope = rememberCoroutineScope()
    
    // 接口检测状态：id -> Pair(是否可用, 提示信息)
    var checkResults by remember { mutableStateOf<Map<String, Pair<Boolean, String>>>(emptyMap()) }
    var isChecking by remember { mutableStateOf(false) }

    /**
     * 检测所有接口可用性
     */
    suspend fun checkAllApis() {
        isChecking = true
        val results = mutableMapOf<String, Pair<Boolean, String>>()
        val enabledMap = mutableMapOf<String, Boolean>()
        for (config in configs) {
            val (available, message) = VideoRepository.checkApiAvailability(config.url)
            results[config.id] = Pair(available, message)
            enabledMap[config.id] = available
            checkResults = results.toMap()
        }
        // 持久化检测结果：失效接口标记为不可用
        configManager.setAllConfigsEnabled(enabledMap)
        configs = configManager.loadConfigs()
        isChecking = false
    }

    LaunchedEffect(Unit) {
        launch(Dispatchers.IO) {
            checkAllApis()
        }
    }

    // 统计有效/失效接口数量
    val validCount = configs.count { config ->
        val result = checkResults[config.id]
        result?.first ?: true // 未检测默认为有效
    }
    val invalidCount = configs.size - validCount

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
            // 标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "自定义接口设置",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // 接口状态卡片
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF252525))
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "接口状态",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (isChecking) {
                                Spacer(modifier = Modifier.size(8.dp))
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color(0xFF25F4EE),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "共 ${configs.size} 个接口",
                            color = Color(0xCCFFFFFF),
                            fontSize = 14.sp
                        )
                        Text(
                            text = "有效: $validCount  失效: $invalidCount",
                            color = if (invalidCount == 0) Color(0xFF25F4EE) else Color(0xFFFF6B6B),
                            fontSize = 12.sp
                        )
                    }
                    Row {
                        // 添加按钮
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF25F4EE))
                                .clickable { showAddSheet = true }
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "添加",
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.size(4.dp))
                                Text(
                                    text = "添加",
                                    color = Color.Black,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Spacer(modifier = Modifier.size(8.dp))
                        // 检测/刷新按钮
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF333333))
                                .clickable {
                                    configs = configManager.loadConfigs()
                                    scope.launch(Dispatchers.IO) {
                                        checkAllApis()
                                    }
                                    Toast.makeText(context, "正在检测接口...", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "检测",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.size(4.dp))
                                Text(
                                    text = "检测",
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            // 接口列表
            Text(
                text = "接口列表",
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                items(configs) { config ->
                    val checkResult = checkResults[config.id]
                    val isAvailable = checkResult?.first
                    
                    ApiConfigItem(
                        config = config,
                        isAvailable = isAvailable,
                        checkMessage = checkResult?.second,
                        onCopy = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("API地址", config.url)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "已复制接口地址", Toast.LENGTH_SHORT).show()
                        },
                        onEdit = { editingConfig = config },
                        onDelete = {
                            configManager.deleteConfig(config.id)
                            configs = configManager.loadConfigs()
                            checkResults = checkResults.toMutableMap().apply { remove(config.id) }
                        }
                    )
                }
            }
        }
    }

    // 添加接口底部弹窗
    if (showAddSheet) {
        ApiConfigBottomSheet(
            title = "添加接口",
            onDismiss = { showAddSheet = false },
            onConfirm = { name, url, icon ->
                configManager.addConfig(name, url, icon)
                configs = configManager.loadConfigs()
                showAddSheet = false
                // 检测新添加的接口
                scope.launch(Dispatchers.IO) {
                    val newConfig = configs.lastOrNull()
                    if (newConfig != null) {
                        val (available, message) = VideoRepository.checkApiAvailability(newConfig.url)
                        checkResults = checkResults.toMutableMap().apply {
                            put(newConfig.id, Pair(available, message))
                        }
                    }
                }
            }
        )
    }

    // 编辑接口底部弹窗
    if (editingConfig != null) {
        ApiConfigBottomSheet(
            title = "编辑接口",
            initialName = editingConfig!!.name,
            initialUrl = editingConfig!!.url,
            initialIcon = editingConfig!!.icon,
            onDismiss = { editingConfig = null },
            onConfirm = { name, url, icon ->
                configManager.updateConfig(editingConfig!!.id, name, url, icon)
                configs = configManager.loadConfigs()
                val editedId = editingConfig!!.id
                editingConfig = null
                // 检测修改后的接口
                scope.launch(Dispatchers.IO) {
                    val editedConfig = configs.find { it.id == editedId }
                    if (editedConfig != null) {
                        val (available, message) = VideoRepository.checkApiAvailability(editedConfig.url)
                        checkResults = checkResults.toMutableMap().apply {
                            put(editedId, Pair(available, message))
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun ApiConfigItem(
    config: ApiConfig,
    isAvailable: Boolean?,
    checkMessage: String?,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF252525))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 图标
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF333333)),
                    contentAlignment = Alignment.Center
                ) {
                    IconForName(
                        iconName = config.icon,
                        tint = Color(0xFF25F4EE),
                        size = 20.dp
                    )
                }
                Spacer(modifier = Modifier.size(12.dp))
                // 名称和URL
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = config.name,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        // 状态指示灯
                        if (isAvailable != null) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (isAvailable) Color(0xFF25F4EE) else Color(0xFFFF6B6B)
                                    )
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Gray)
                            )
                        }
                    }
                    Text(
                        text = config.url,
                        color = Color(0x99FFFFFF),
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
                // 操作按钮
                Row {
                    IconButton(onClick = onCopy) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "复制",
                            tint = Color(0xCCFFFFFF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "编辑",
                            tint = Color(0xCCFFFFFF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "删除",
                            tint = Color(0xFFFF6B6B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            // 检测结果提示
            if (isAvailable != null && checkMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isAvailable) "✓ 接口可用" else "✗ $checkMessage",
                    color = if (isAvailable) Color(0xFF25F4EE) else Color(0xFFFF6B6B),
                    fontSize = 11.sp
                )
            }
        }
    }
}

/**
 * 图标选择网格组件
 */
@Composable
fun IconPickerGrid(
    selectedIcon: String,
    onIconSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
    ) {
        items(ApiIcons.icons) { iconName ->
            val isSelected = iconName == selectedIcon
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) Color(0xFF25F4EE) else Color(0xFF333333)
                    )
                    .clickable { onIconSelected(iconName) },
                contentAlignment = Alignment.Center
            ) {
                IconForName(
                    iconName = iconName,
                    tint = if (isSelected) Color.Black else Color(0xCCFFFFFF),
                    size = 22.dp
                )
            }
        }
    }
}

/**
 * 根据图标名称显示对应emoji
 */
@Composable
fun IconForName(iconName: String, tint: Color, size: androidx.compose.ui.unit.Dp) {
    val emoji = when (iconName) {
        "smile" -> "\uD83D\uDE0A"
        "heart" -> "\u2764\uFE0F"
        "star" -> "\u2B50"
        "thumb" -> "\uD83D\uDC4D"
        "gem" -> "\uD83D\uDC8E"
        "sparkle" -> "\u2728"
        "fire" -> "\uD83D\uDD25"
        "bolt" -> "\u26A1"
        "target" -> "\uD83C\uDFAF"
        "flower" -> "\uD83C\uDF38"
        "moon" -> "\uD83C\uDF19"
        "sun" -> "\u2600\uFE0F"
        "crown" -> "\uD83D\uDC51"
        "diamond" -> "\uD83D\uDCA0"
        "rocket" -> "\uD83D\uDE80"
        "globe" -> "\uD83C\uDF10"
        "shield" -> "\uD83D\uDEE1"
        "compass" -> "\uD83E\uDDED"
        "trophy" -> "\uD83C\uDFC6"
        "wave" -> "\uD83C\uDF0A"
        else -> "\uD83D\uDE0A"
    }
    Text(
        text = emoji,
        fontSize = size.value.sp,
        color = tint
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApiConfigBottomSheet(
    title: String,
    initialName: String = "",
    initialUrl: String = "",
    initialIcon: String = "smile",
    onDismiss: () -> Unit,
    onConfirm: (name: String, url: String, icon: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var url by remember { mutableStateOf(initialUrl) }
    var selectedIcon by remember { mutableStateOf(initialIcon) }
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1E1E1E)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "添加后会显示在视频接口入口",
                color = Color(0x99FFFFFF),
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { 
                    if (it.length <= 4) name = it 
                },
                label = { Text("接口名称", color = Color(0x99FFFFFF)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF25F4EE),
                    unfocusedBorderColor = Color(0xFF444444)
                ),
                supportingText = {
                    Text(
                        text = "名称不超过4个字 (${name.length}/4)",
                        color = Color(0x66FFFFFF),
                        fontSize = 11.sp
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("接口地址", color = Color(0x99FFFFFF)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF25F4EE),
                    unfocusedBorderColor = Color(0xFF444444)
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "选择图标",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${ApiIcons.icons.size} 个可选",
                    color = Color(0x66FFFFFF),
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            IconPickerGrid(
                selectedIcon = selectedIcon,
                onIconSelected = { selectedIcon = it },
                modifier = Modifier.height(200.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "接口需返回302重定向到视频地址",
                color = Color(0x66FFFFFF),
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", color = Color(0x99FFFFFF), fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.size(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF25F4EE))
                        .clickable {
                            if (name.isNotBlank() && url.isNotBlank()) {
                                onConfirm(name, url, selectedIcon)
                            }
                        }
                        .padding(horizontal = 32.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "确定",
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
