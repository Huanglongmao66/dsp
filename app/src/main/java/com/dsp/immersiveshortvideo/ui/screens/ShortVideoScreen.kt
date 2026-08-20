package com.dsp.immersiveshortvideo.ui.screens

import android.app.Activity
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dsp.immersiveshortvideo.data.ApiConfig
import com.dsp.immersiveshortvideo.data.ApiConfigManager
import com.dsp.immersiveshortvideo.player.DownloadStatus
import com.dsp.immersiveshortvideo.player.PlayerViewModel
import com.dsp.immersiveshortvideo.player.VideosUiState
import com.dsp.immersiveshortvideo.ui.components.FloatingHeartAnimation
import com.dsp.immersiveshortvideo.ui.components.GestureIndicatorOverlay
import com.dsp.immersiveshortvideo.ui.components.GestureIndicatorState
import com.dsp.immersiveshortvideo.ui.components.VideoPlayer
import com.dsp.immersiveshortvideo.ui.components.VideoProgressBar
import com.dsp.immersiveshortvideo.ui.components.VideoSettingsSheet
import com.dsp.immersiveshortvideo.ui.components.videoGestureDetector
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 沉浸式短视频主界面
 *
 * 架构（参考抖音竖屏全屏布局规范）：
 * - 外层：Box (全屏黑色背景)
 *   ├─ VerticalPager (视频播放层，带手势检测)
 *   │   └─ 每页：VideoPage (2层：视频 + 手势)
 *   └─ 全局UI浮层 (Pager外部，单实例)
 *       ├─ 左上角菜单按钮
 *       ├─ 右侧互动面板(3按钮)
 *       ├─ 右下角眼睛按钮
 *       ├─ 底部进度条
 *       └─ 中央播放/暂停反馈
 *
 * 优势：
 * - 页面Item仅2层，滑动帧率稳定
 * - UI浮层不随Pager重建，状态一致
 * - 手势检测只在视频层，不与UI按钮冲突
 */
@Composable
fun ShortVideoScreen(
    viewModel: PlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    val screenPrefs = remember { context.getSharedPreferences("screen_prefs", Context.MODE_PRIVATE) }

    // 读取持久化的分类选择
    val savedCategoryName = remember { screenPrefs.getString("category_name", "默认推荐") ?: "默认推荐" }
    val savedCategoryUrl = remember { screenPrefs.getString("category_url", null) }
    var selectedCategoryName by remember { mutableStateOf(savedCategoryName) }

    LaunchedEffect(Unit) {
        viewModel.preparePlayers(context)
        viewModel.loadVideosFromApi(count = 12, apiUrl = savedCategoryUrl)
    }

    val systemUi = rememberSystemUiController()
    DisposableEffect(systemUi) {
        systemUi.isStatusBarVisible = false
        systemUi.isNavigationBarVisible = false
        onDispose { }
    }
    val activity = context as? Activity

    val videosState by viewModel.videosState.collectAsStateWithLifecycle()
    val currentIndex by viewModel.currentIndex.collectAsStateWithLifecycle()
    val likedMap by viewModel.likedMap.collectAsStateWithLifecycle()
    val progressMap by viewModel.progressMap.collectAsStateWithLifecycle()
    val durationMap by viewModel.durationMap.collectAsStateWithLifecycle()
    val doubleTapEvent by viewModel.doubleTapLikeEvent.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val autoPlayNext by viewModel.autoPlayNext.collectAsStateWithLifecycle()
    val currentSpeed by viewModel.currentSpeed.collectAsStateWithLifecycle()
    val autoPlayScrollEvent by viewModel.autoPlayScrollEvent.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val cacheSizeText by viewModel.cacheSizeText.collectAsStateWithLifecycle()
    val uiMessage by viewModel.uiMessage.collectAsStateWithLifecycle()
    val scaleMode by viewModel.scaleMode.collectAsStateWithLifecycle()
    val videoPixelSize by viewModel.videoPixelSize.collectAsStateWithLifecycle()
    val scaleModeOptions = viewModel.scaleModeOptions

    val videos = (videosState as? VideosUiState.Success)?.videos.orEmpty()
    val isLoadingMore = (videosState as? VideosUiState.Success)?.isLoadingMore == true
    val pagerState = rememberPagerState(pageCount = { videos.size })
    val scope = rememberCoroutineScope()

    var gestureIndicator by remember { mutableStateOf(GestureIndicatorState()) }
    var screenSize by remember { mutableStateOf(IntSize.Zero) }
    var hideAllUi by remember { mutableStateOf(false) }
    var lastEyeClickTime by remember { mutableLongStateOf(0L) }
    var showSheet by remember { mutableStateOf(false) }
    var showCategoryPage by remember { mutableStateOf(false) }
    var showApiSettings by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.settledPage, videos.size) {
        val settled = pagerState.settledPage
        if (videos.isNotEmpty() && settled != currentIndex && settled in videos.indices) {
            viewModel.switchToPage(settled, currentIndex, videos)
            viewModel.checkAndLoadMore(settled, threshold = 3)
        }
    }

    // 打开设置面板时刷新缓存大小
    LaunchedEffect(showSheet) {
        if (showSheet) viewModel.refreshCacheSize(context)
    }

    // 监听 VM UI 消息并显示 Toast
    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            showToast(context, it)
            viewModel.consumeUiMessage()
        }
    }

    LaunchedEffect(autoPlayScrollEvent) {
        val target = autoPlayScrollEvent ?: return@LaunchedEffect
        if (target in 0 until videos.size && target != pagerState.currentPage) {
            pagerState.animateScrollToPage(target)
        }
        viewModel.consumeAutoPlayScrollEvent()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onGloballyPositioned { screenSize = it.size }
    ) {
        when (val s = videosState) {
            is VideosUiState.Loading -> LoadingView()
            is VideosUiState.Error -> ErrorView(s.message) { viewModel.loadVideosFromApi(12, savedCategoryUrl) }
            is VideosUiState.Success -> {
                if (s.videos.isEmpty()) {
                    EmptyView()
                } else {
                    // ====== 视频播放层 ======
                    VerticalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        beyondBoundsPageCount = 1,
                        key = { pageIndex -> videos[pageIndex].id }
                    ) { pageIndex ->
                        VideoPage(
                            video = s.videos[pageIndex],
                            relIndex = pageIndex - currentIndex,
                            isCurrentPage = pageIndex == currentIndex,
                            screenSize = screenSize,
                            activity = activity,
                            context = context,
                            viewModel = viewModel,
                            scaleMode = scaleMode,
                            videoPixelSize = videoPixelSize,
                            onSingleTap = {
                                // 点击屏幕不再暂停播放，仅通过右侧按钮控制
                            },
                            onShowGesture = { state ->
                                scope.launch {
                                    gestureIndicator = state
                                    delay(700)
                                    gestureIndicator = GestureIndicatorState()
                                }
                            },
                            onShowLongPress = {
                                scope.launch {
                                    delay(250)
                                    if (gestureIndicator.text.contains("长按加速")) {
                                        gestureIndicator = GestureIndicatorState()
                                    }
                                }
                            }
                        )
                    }

                    // ====== 全局UI浮层 ======
                    val currentVideo = videos.getOrNull(currentIndex)
                    if (currentVideo != null) {
                        AnimatedVisibility(
                            visible = !hideAllUi,
                            enter = fadeIn(),
                            exit = fadeOut(),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            GlobalOverlayUI(
                                video = currentVideo,
                                isLiked = likedMap[currentVideo.id] == true,
                                isDownloaded = downloadState[currentVideo.id] == DownloadStatus.Downloaded,
                                isDownloading = downloadState[currentVideo.id] == DownloadStatus.Downloading,
                                isPlaying = isPlaying,
                                onMenuClick = { showCategoryPage = true },
                                onLikeClick = { viewModel.toggleLike(currentVideo.id) },
                                onDownloadClick = {
                                    viewModel.downloadVideo(context, currentVideo.id, currentVideo.videoUrl)
                                },
                                onExpandClick = { showSheet = true },
                                onPlayPauseClick = {
                                    viewModel.togglePlayPause()
                                },
                                progressMs = progressMap[currentVideo.id] ?: 0L,
                                durationMs = durationMap[currentVideo.id]?.takeIf { it > 0L },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // 2. 右下角眼睛按钮（始终可见）
                        IconButton(
                            onClick = {
                                val now = System.currentTimeMillis()
                                if (now - lastEyeClickTime >= 300) {
                                    lastEyeClickTime = now
                                    hideAllUi = !hideAllUi
                                    showToast(context, if (hideAllUi) "纯净模式" else "已显示UI")
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 16.dp, bottom = 100.dp)
                                .size(44.dp)
                        ) {
                            Icon(
                                imageVector = if (hideAllUi) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = "纯净模式",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    } else {
                        // currentVideo 为 null 时跳过 UI 渲染
                    }

                    // ===== 双击飘心动画 =====
                    doubleTapEvent?.let { pair ->
                        FloatingHeartAnimation(
                            anchorX = pair.first,
                            anchorY = pair.second,
                            onAnimationEnd = { }
                        )
                    }

                    // ===== 手势指示器 =====
                    GestureIndicatorOverlay(state = gestureIndicator)

                    // ===== 半屏设置菜单 =====
                    if (showSheet && currentVideo != null) {
                        VideoSettingsSheet(
                            onDismiss = { showSheet = false },
                            isAutoPlayNext = autoPlayNext,
                            currentSpeed = currentSpeed,
                            speedOptions = viewModel.speedOptions,
                            scaleMode = scaleMode,
                            scaleModeOptions = scaleModeOptions,
                            isHideAllUi = hideAllUi,
                            isDownloaded = downloadState[currentVideo.id] == DownloadStatus.Downloaded,
                            isDownloading = downloadState[currentVideo.id] == DownloadStatus.Downloading,
                            cacheSizeText = cacheSizeText,
                            onAutoPlayToggle = {
                                val newVal = viewModel.toggleAutoPlayNext()
                                showToast(context, if (newVal) "自动播放已开启" else "已切换手动模式")
                                scope.launch {
                                    gestureIndicator = GestureIndicatorState(
                                        visible = true,
                                        icon = if (newVal) "▶" else "⏸",
                                        text = if (newVal) "自动播放" else "手动模式"
                                    )
                                    delay(1000)
                                    gestureIndicator = GestureIndicatorState()
                                }
                            },
                            onSpeedChange = { speed ->
                                viewModel.setSpeed(speed)
                                showToast(context, "已切换 ${speed}x")
                            },
                            onScaleModeChange = { mode ->
                                viewModel.setScaleMode(mode)
                                showToast(context, "画面模式：${mode.displayName}")
                            },
                            onDownload = {
                                viewModel.downloadVideo(context, currentVideo.id, currentVideo.videoUrl)
                            },
                            onClearScreen = {
                                hideAllUi = !hideAllUi
                                showToast(context, if (hideAllUi) "纯净模式" else "已显示UI")
                            },
                            onCopyUrl = {
                                viewModel.copyVideoUrl(context, currentVideo.videoUrl)
                                showToast(context, "链接已复制")
                            },
                            onClearCache = {
                                viewModel.clearCache(context) { success, msg ->
                                    showToast(context, msg)
                                    if (success) viewModel.refreshCacheSize(context)
                                }
                            }
                        )
                    }

                    // ===== 底部加载更多 =====
                    if (isLoadingMore) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(
                                color = Color(0xFFFE2C55),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // ===== 分类页面（全屏覆盖）=====
        if (showCategoryPage) {
            CategoryPage(
                onDismiss = { showCategoryPage = false },
                onCategorySelected = { apiConfig ->
                    selectedCategoryName = apiConfig?.name ?: "默认推荐"
                    // 持久化分类选择
                    screenPrefs.edit()
                        .putString("category_name", selectedCategoryName)
                        .putString("category_url", apiConfig?.url)
                        .apply()
                    viewModel.loadVideosFromApi(
                        count = 12,
                        apiUrl = apiConfig?.url
                    )
                    showCategoryPage = false
                },
                onOpenApiSettings = {
                    showCategoryPage = false
                    showApiSettings = true
                },
                currentCategoryName = selectedCategoryName
            )
        }

        // ===== API设置页面（全屏覆盖）=====
        if (showApiSettings) {
            ApiSettingsPage(
                onDismiss = { showApiSettings = false }
            )
        }
    }
}

@Composable
private fun VideoPage(
    video: com.dsp.immersiveshortvideo.model.ShortVideo,
    relIndex: Int,
    isCurrentPage: Boolean,
    screenSize: IntSize,
    activity: Activity?,
    context: Context,
    viewModel: PlayerViewModel,
    scaleMode: com.dsp.immersiveshortvideo.player.VideoScaleMode,
    videoPixelSize: Pair<Int, Int>,
    onSingleTap: () -> Unit,
    onShowGesture: (GestureIndicatorState) -> Unit,
    onShowLongPress: () -> Unit
) {
    val player = viewModel.getPlayerForIndex(relIndex)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .videoGestureDetector(
                screenSizePx = screenSize,
                edgeThresholdPx = 120f,
                onSingleTap = { if (isCurrentPage) onSingleTap() },
                onDoubleTap = { x, y ->
                    if (isCurrentPage) viewModel.triggerDoubleTapLike(video.id, x, y, listOf(video))
                },
                onHorizontalDragEnd = { totalDx ->
                    if (isCurrentPage && screenSize.width > 0) {
                        val seconds = (totalDx / screenSize.width * 10f).toInt()
                        player?.let { exo ->
                            val newPos = (exo.currentPosition + seconds * 1000L)
                                .coerceIn(0L, exo.duration.coerceAtLeast(1L))
                            exo.seekTo(newPos)
                        }
                        onShowGesture(
                            GestureIndicatorState(
                                visible = true,
                                icon = if (seconds >= 0) "⏩" else "⏪",
                                text = "${if (seconds >= 0) "+" else ""}$seconds 秒"
                            )
                        )
                    }
                },
                onVerticalDragEnd = { isLeftEdge, totalDy ->
                    if (isCurrentPage && screenSize.height > 0) {
                        val changePercent = (totalDy / screenSize.height) * -1.8f
                        if (isLeftEdge) {
                            val win = activity?.window
                            win?.let { w ->
                                val cur = w.attributes.screenBrightness.takeIf { it >= 0f } ?: 0.5f
                                val newVal = (cur + changePercent).coerceIn(0f, 1f)
                                w.attributes = w.attributes.apply { screenBrightness = newVal }
                                onShowGesture(
                                    GestureIndicatorState(
                                        visible = true, icon = "☀️",
                                        text = "亮度 ${(newVal * 100).toInt()}%",
                                        progress = newVal
                                    )
                                )
                            }
                        } else {
                            val am = context.getSystemService(Context.AUDIO_SERVICE)
                                as android.media.AudioManager
                            val maxVol = am.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                            val curVol = am.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
                            val delta = (changePercent * maxVol).toInt()
                            val newVol = (curVol + delta).coerceIn(0, maxVol)
                            am.setStreamVolume(
                                android.media.AudioManager.STREAM_MUSIC,
                                newVol,
                                android.media.AudioManager.FLAG_SHOW_UI
                            )
                            onShowGesture(
                                GestureIndicatorState(
                                    visible = true,
                                    icon = if (newVol == 0) "🔇" else "🔊",
                                    text = "音量 ${(newVol * 100 / maxVol.coerceAtLeast(1))}%",
                                    progress = newVol.toFloat() / maxVol.coerceAtLeast(1)
                                )
                            )
                        }
                    }
                },
                onLongPress = { _, _ ->
                    if (isCurrentPage) {
                        viewModel.startLongPressSpeed(2.0f)
                        onShowGesture(
                            GestureIndicatorState(
                                visible = true, icon = "⚡",
                                text = "长按加速 2.0x", progress = 1f
                            )
                        )
                    }
                },
                onLongPressEnd = {
                    if (isCurrentPage) {
                        viewModel.stopLongPressSpeed()
                        onShowLongPress()
                    }
                }
            )
    ) {
        VideoPlayer(
            video = video,
            player = player,
            isActivePage = isCurrentPage,
            scaleMode = scaleMode,
            videoPixelSize = videoPixelSize
        )
    }
}

/**
 * 全局悬浮UI层
 * 右侧4按钮（爱心/播放暂停/下载/展开）+ 左上角菜单 + 底部进度条
 */
@Composable
private fun GlobalOverlayUI(
    video: com.dsp.immersiveshortvideo.model.ShortVideo,
    isLiked: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    isPlaying: Boolean,
    onMenuClick: () -> Unit,
    onLikeClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onExpandClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    progressMs: Long,
    durationMs: Long? = null,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // 左上角菜单按钮
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 14.dp, top = 20.dp)
                .size(44.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = "菜单",
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }

        // 右侧4按钮（爱心/播放暂停/下载/展开）
        RightColumnButtons(
            isLiked = isLiked,
            isDownloaded = isDownloaded,
            isDownloading = isDownloading,
            isPlaying = isPlaying,
            onLikeClick = onLikeClick,
            onDownloadClick = onDownloadClick,
            onExpandClick = onExpandClick,
            onPlayPauseClick = onPlayPauseClick,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
        )

        // 底部进度条（贴最底部）：优先使用 ExoPlayer 真实时长，其次模型默认值，最后兜底 15s
        val duration = durationMs?.takeIf { it > 0L }
            ?: video.durationMs.takeIf { it > 0L }
            ?: 15000L
        VideoProgressBar(
            progressMs = progressMs,
            durationMs = duration,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        )
    }
}

/**
 * 右侧按钮列（爱心/播放暂停/下载/展开）
 * 独立组件，确保不被手势检测干扰
 */
@Composable
private fun RightColumnButtons(
    isLiked: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    isPlaying: Boolean,
    onLikeClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onExpandClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(24.dp)
    ) {
        // 爱心
        IconButton(
            onClick = onLikeClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = "点赞",
                tint = if (isLiked) Color(0xFFFE2C55) else Color.White,
                modifier = Modifier.size(30.dp)
            )
        }

        // 播放/暂停（中间空位）
        IconButton(
            onClick = onPlayPauseClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "暂停" else "播放",
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
        }

        // 下载
        IconButton(
            onClick = onDownloadClick,
            enabled = !isDownloading,
            modifier = Modifier.size(48.dp)
        ) {
            if (isDownloading) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = Color(0xFF25F4EE),
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = "下载",
                    tint = if (isDownloaded) Color(0xFF25F4EE) else Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        // 展开
        IconButton(
            onClick = onExpandClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = "更多",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            androidx.compose.material3.CircularProgressIndicator(
                color = Color(0xFFFE2C55),
                strokeWidth = 4.dp,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.padding(vertical = 12.dp))
            androidx.compose.material3.Text(
                text = "正在加载视频…",
                color = Color(0xCCFFFFFF),
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun ErrorView(msg: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            androidx.compose.material3.Text(text = "⚠️", fontSize = 48.sp)
            Spacer(modifier = Modifier.padding(vertical = 12.dp))
            androidx.compose.material3.Text(text = msg, color = Color(0xCCFFFFFF))
            Spacer(modifier = Modifier.padding(vertical = 12.dp))
            androidx.compose.material3.Button(
                onClick = onRetry,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFE2C55),
                    contentColor = Color.White
                )
            ) {
                androidx.compose.material3.Text(text = "重新加载")
            }
        }
    }
}

@Composable
private fun EmptyView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text("暂无视频数据", color = Color.White)
    }
}

private fun showToast(context: Context, message: String) {
    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
}
