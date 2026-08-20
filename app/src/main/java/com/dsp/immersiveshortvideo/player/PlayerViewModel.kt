package com.dsp.immersiveshortvideo.player

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.dsp.immersiveshortvideo.data.VideoRepository
import com.dsp.immersiveshortvideo.model.ShortVideo
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

private const val TAG = "PlayerVM"
private const val MAX_PLAYER_ERROR_RETRY = 3
private const val HEART_ANIM_DURATION_MS = 1200L
private const val PROGRESS_POLL_INTERVAL_MS = 200L
private const val CACHE_MAX_SIZE = 200L * 1024 * 1024

/** 下载状态三态 */
enum class DownloadStatus { Downloading, Downloaded }

/**
 * 视频列表加载状态
 */
sealed interface VideosUiState {
    data object Loading : VideosUiState
    data class Success(
        val videos: List<ShortVideo>,
        val isLoadingMore: Boolean = false,
        val hasMore: Boolean = true
    ) : VideosUiState
    data class Error(val message: String) : VideosUiState
}

/**
 * 播放器ViewModel - 核心播放控制
 *
 * 3实例ExoPlayer池（Top/Center/Bottom）秒切 + 本地磁盘LRU缓存预加载 + 速度切换/长按加速 + 自动下一条。
 */
class PlayerViewModel : ViewModel() {

    // ===== 播放器池：3个实例循环复用 =====
    private var playerTop: ExoPlayer? = null
    private var playerCenter: ExoPlayer? = null
    private var playerBottom: ExoPlayer? = null

    // ===== 磁盘缓存 =====
    private var simpleCache: SimpleCache? = null
    private var cacheDataSourceFactory: CacheDataSource.Factory? = null
    private var httpFactory: DefaultHttpDataSource.Factory? = null
    private var mediaSourceFactory: ProgressiveMediaSource.Factory? = null

    // ===== 视频列表加载状态 =====
    private val _videosState = MutableStateFlow<VideosUiState>(VideosUiState.Loading)
    val videosState: StateFlow<VideosUiState> = _videosState.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _likedMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val likedMap: StateFlow<Map<String, Boolean>> = _likedMap.asStateFlow()

    private val _progressMap = MutableStateFlow<Map<String, Long>>(emptyMap())
    val progressMap: StateFlow<Map<String, Long>> = _progressMap.asStateFlow()

    private val _durationMap = MutableStateFlow<Map<String, Long>>(emptyMap())
    val durationMap: StateFlow<Map<String, Long>> = _durationMap.asStateFlow()

    private val _doubleTapLikeEvent = MutableStateFlow<Pair<Float, Float>?>(null)
    val doubleTapLikeEvent: StateFlow<Pair<Float, Float>?> = _doubleTapLikeEvent.asStateFlow()

    private val _isPlaying = MutableStateFlow(true)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    val speedOptions: List<Float> = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f)

    private val _currentSpeed = MutableStateFlow(1.0f)
    val currentSpeed: StateFlow<Float> = _currentSpeed.asStateFlow()

    @Volatile
    private var longPressSpeedActive: Float? = null

    private val _autoPlayNext = MutableStateFlow(false)
    val autoPlayNext: StateFlow<Boolean> = _autoPlayNext.asStateFlow()

    // ===== 画面缩放模式（持久化） =====
    val scaleModeOptions: List<VideoScaleMode> = listOf(
        VideoScaleMode.DEFAULT,
        VideoScaleMode.FIT,
        VideoScaleMode.FILL,
        VideoScaleMode.ADAPTIVE
    )
    private val _scaleMode = MutableStateFlow(VideoScaleMode.DEFAULT)
    val scaleMode: StateFlow<VideoScaleMode> = _scaleMode.asStateFlow()

    /** 当前视频帧原始尺寸（像素）：用于"原始尺寸"模式下限定 Surface 大小；不存在时 0 */
    private val _videoPixelSize = MutableStateFlow(Pair(0, 0))
    val videoPixelSize: StateFlow<Pair<Int, Int>> = _videoPixelSize.asStateFlow()

    @Volatile
    private var prefs: android.content.SharedPreferences? = null
    private val prefKeyScaleMode = "scale_mode_name"

    private val _autoPlayScrollEvent = MutableStateFlow<Int?>(null)
    val autoPlayScrollEvent: StateFlow<Int?> = _autoPlayScrollEvent.asStateFlow()

    @Volatile
    private var lastCompletedVideoId: String? = null

    @Volatile
    private var currentApiUrl: String? = null

    private var progressJob: Job? = null
    private var doubleTapJob: Job? = null
    private var playerErrorRetryCount = 0

    // ===== UI 事件（替代直接 Toast） =====
    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    fun consumeUiMessage() { _uiMessage.value = null }
    private fun sendMessage(msg: String) { _uiMessage.value = msg }

    /**
     * 初始化3个播放器实例 + 本地磁盘缓存
     */
    fun preparePlayers(context: Context) {
        if (playerCenter != null) return
        val appCtx = context.applicationContext

        // 初始化持久化（用于缩放模式等 UI 偏好）
        if (prefs == null) {
            prefs = appCtx.getSharedPreferences("player_prefs", Context.MODE_PRIVATE)
            _scaleMode.value = VideoScaleMode.fromName(prefs?.getString(prefKeyScaleMode, null))
        }

        buildCache(appCtx)

        val http = DefaultHttpDataSource.Factory()
            .setUserAgent("ImmersiveShortVideo/1.0")
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(15_000)
            .setAllowCrossProtocolRedirects(true)
        httpFactory = http

        val cacheFactory = buildCacheDataSourceFactory()
        val upstream: androidx.media3.datasource.DataSource.Factory = cacheFactory ?: http
        mediaSourceFactory = ProgressiveMediaSource.Factory(upstream)

        playerTop = createPlayer(appCtx)
        playerCenter = createPlayer(appCtx)
        playerBottom = createPlayer(appCtx)
        applySpeedToAllPlayers()
    }

    private fun buildCache(appCtx: Context) {
        runCatching {
            val cacheDir = File(appCtx.cacheDir, "video_cache").apply { mkdirs() }
            simpleCache = SimpleCache(cacheDir, LeastRecentlyUsedCacheEvictor(CACHE_MAX_SIZE))
        }.onFailure { Log.e(TAG, "缓存初始化失败", it) }
    }

    private fun buildCacheDataSourceFactory(): CacheDataSource.Factory? = runCatching {
        CacheDataSource.Factory()
            .setCache(simpleCache ?: return@runCatching null)
            .setUpstreamDataSourceFactory(httpFactory ?: return@runCatching null)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }.getOrNull()

    private fun createPlayer(appCtx: Context): ExoPlayer {
        val factory = mediaSourceFactory ?: ProgressiveMediaSource.Factory(
            httpFactory ?: DefaultHttpDataSource.Factory()
        )
        return ExoPlayer.Builder(appCtx)
            .setMediaSourceFactory(factory)
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_OFF
                volume = 1f
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        Log.e(TAG, "播放错误: ${error.errorCodeName}", error)
                        if (this@apply !== playerCenter) return
                        if (playerErrorRetryCount >= MAX_PLAYER_ERROR_RETRY) {
                            Log.w(TAG, "超过最大重试次数($MAX_PLAYER_ERROR_RETRY)，跳过")
                            sendMessage("视频加载失败，请切换下一个")
                            playerErrorRetryCount = 0
                            return
                        }
                        playerErrorRetryCount++
                        val backoff = 1000L * (1 shl (playerErrorRetryCount - 1))
                        viewModelScope.launch {
                            delay(backoff)
                            runCatching {
                                this@apply.prepare()
                                this@apply.playWhenReady = true
                            }.onFailure { Log.e(TAG, "重试失败", it) }
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (this@apply !== playerCenter) return
                        // 记录真实时长（当缓冲完成/准备好后才有 duration）
                        if (playbackState == Player.STATE_READY) {
                            val state = _videosState.value as? VideosUiState.Success
                            val video = state?.videos?.getOrNull(_currentIndex.value)
                            val dur = this@apply.duration
                            if (video != null && dur > 0L) {
                                _durationMap.update { it + (video.id to dur) }
                            }
                            // 记录视频原始像素尺寸（原始尺寸模式会用到）
                            val format: androidx.media3.common.Format? = videoFormat
                            val w = format?.width ?: 0
                            val h = format?.height ?: 0
                            if (w > 0 && h > 0) _videoPixelSize.value = Pair(w, h)
                        }
                        if (playbackState != Player.STATE_ENDED) return
                        playerErrorRetryCount = 0
                        val state = _videosState.value as? VideosUiState.Success ?: return
                        val video = state.videos.getOrNull(_currentIndex.value) ?: return
                        if (video.id == lastCompletedVideoId) return
                        lastCompletedVideoId = video.id
                        // STATE_ENDED 时把进度强制设置到末尾，避免进度条停在一半
                        _progressMap.update { it + (video.id to this@apply.duration.coerceAtLeast(0L)) }
                        if (_autoPlayNext.value) {
                            val next = _currentIndex.value + 1
                            if (next in state.videos.indices) {
                                viewModelScope.launch {
                                    delay(150)
                                    _autoPlayScrollEvent.value = next
                                }
                            } else {
                                this@apply.seekTo(0)
                                this@apply.playWhenReady = true
                                lastCompletedVideoId = null
                            }
                        } else {
                            pause()
                        }
                    }
                })
            }
    }

    /** 预加载单个 player 的 MediaItem */
    private fun preloadPlayer(player: ExoPlayer?, videoUrl: String, play: Boolean = false) {
        player?.let { p ->
            val current = p.currentMediaItem?.mediaId ?: ""
            if (current != videoUrl) {
                Log.d(TAG, "preloadPlayer: 切换视频 $current -> $videoUrl, 旧state=${p.playbackState}")
                // 切换了视频源：必须 setMediaItem + prepare，
                // 否则若 player 之前已播放过（STATE_READY/ENDED），不会自动缓冲新视频
                p.setMediaItem(MediaItem.fromUri(videoUrl))
                p.prepare()
            }
            p.seekTo(0)
            p.playWhenReady = play
        }
    }

    fun loadInitialVideos(videos: List<ShortVideo>) {
        if (videos.isEmpty()) return
        lastCompletedVideoId = null
        Log.d(TAG, "loadInitialVideos: 首个=${videos[0].videoUrl}, playerCenter state=${playerCenter?.playbackState}")
        preloadPlayer(playerCenter, videos[0].videoUrl, play = true)
        _isPlaying.value = true
        if (videos.size > 1) preloadPlayer(playerBottom, videos[1].videoUrl)
        playerTop?.stop()
        playerTop?.clearMediaItems()
        startProgressLoop()
    }

    fun getPlayerForIndex(relativeIndex: Int): ExoPlayer? = when (relativeIndex) {
        0 -> playerCenter
        1 -> playerBottom
        -1 -> playerTop
        else -> null
    }

    fun switchToPage(newIndex: Int, oldIndex: Int, videos: List<ShortVideo>) {
        if (newIndex == oldIndex || videos.isEmpty()) return
        val direction = if (newIndex > oldIndex) 1 else -1

        playerCenter?.playWhenReady = false
        playerCenter?.seekTo(0)

        val oldCenter = playerCenter
        val oldTop = playerTop
        val oldBottom = playerBottom

        if (direction == 1) {
            playerTop = oldCenter
            playerCenter = oldBottom
            playerBottom = oldTop
        } else {
            playerBottom = oldCenter
            playerCenter = oldTop
            playerTop = oldBottom
        }

        playerCenter?.let { p ->
            p.playWhenReady = true
            // 若新页 player 还未 prepare（例如预加载失败/被 stop），这里兜底 prepare
            if (p.playbackState == Player.STATE_IDLE ||
                p.playbackState == Player.STATE_ENDED
            ) p.prepare()
        }
        _isPlaying.value = true

        val nextTopIndex = newIndex - 1
        val nextBottomIndex = newIndex + 1
        if (nextTopIndex in videos.indices) {
            preloadPlayer(playerTop, videos[nextTopIndex].videoUrl)
        } else {
            playerTop?.stop()
            playerTop?.clearMediaItems()
        }
        if (nextBottomIndex in videos.indices) {
            preloadPlayer(playerBottom, videos[nextBottomIndex].videoUrl)
        } else {
            playerBottom?.stop()
            playerBottom?.clearMediaItems()
        }
        lastCompletedVideoId = null
        _currentIndex.value = newIndex
    }

    fun togglePlayPause() {
        val p = playerCenter ?: return
        if (p.playWhenReady) {
            p.playWhenReady = false
            _isPlaying.value = false
        } else {
            if (p.playbackState == Player.STATE_ENDED) {
                p.seekTo(0)
                lastCompletedVideoId = null
            }
            if (p.playbackState == Player.STATE_IDLE) p.prepare()
            p.playWhenReady = true
            _isPlaying.value = true
        }
    }

    fun pause() {
        playerCenter?.playWhenReady = false
        _isPlaying.value = false
    }
    fun play() {
        val p = playerCenter ?: return
        if (p.playbackState == Player.STATE_ENDED) { p.seekTo(0); lastCompletedVideoId = null }
        if (p.playbackState == Player.STATE_IDLE) p.prepare()
        p.playWhenReady = true
        _isPlaying.value = true
    }

    fun triggerDoubleTapLike(videoId: String, x: Float, y: Float, videos: List<ShortVideo>) {
        _likedMap.update { it + (videoId to true) }
        _doubleTapLikeEvent.value = Pair(x, y)
        doubleTapJob?.cancel()
        doubleTapJob = viewModelScope.launch {
            delay(HEART_ANIM_DURATION_MS)
            _doubleTapLikeEvent.value = null
        }
    }

    fun toggleLike(videoId: String) {
        _likedMap.update { it + (videoId to !(it[videoId] ?: false)) }
    }

    fun toggleAutoPlayNext(): Boolean {
        val newVal = !_autoPlayNext.value
        _autoPlayNext.value = newVal
        return newVal
    }
    fun setAutoPlayNext(value: Boolean) { _autoPlayNext.value = value }

    fun setSpeed(speed: Float) {
        _currentSpeed.value = speed.coerceIn(0.25f, 4f)
        longPressSpeedActive = null
        applySpeedToAllPlayers()
    }

    fun setScaleMode(mode: VideoScaleMode) {
        _scaleMode.value = mode
        runCatching {
            prefs?.edit()?.putString(prefKeyScaleMode, mode.name)?.apply()
        }
    }

    fun startLongPressSpeed(targetSpeed: Float = 2.0f) {
        if (longPressSpeedActive == targetSpeed) return
        longPressSpeedActive = targetSpeed
        applySpeedToAllPlayers()
    }
    fun stopLongPressSpeed() {
        if (longPressSpeedActive == null) return
        longPressSpeedActive = null
        applySpeedToAllPlayers()
    }
    val effectiveSpeed: Float
        get() = longPressSpeedActive ?: _currentSpeed.value

    private fun applySpeedToAllPlayers() {
        val speed = effectiveSpeed
        playerTop?.playbackParameters = PlaybackParameters(speed, 1.0f)
        playerCenter?.playbackParameters = PlaybackParameters(speed, 1.0f)
        playerBottom?.playbackParameters = PlaybackParameters(speed, 1.0f)
    }

    private fun startProgressLoop() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                val idx = _currentIndex.value
                val videos = (_videosState.value as? VideosUiState.Success)?.videos.orEmpty()
                val video = videos.getOrNull(idx)
                val player = playerCenter
                if (video != null && player != null) {
                    val pos = player.currentPosition.coerceAtLeast(0L)
                    val dur = player.duration
                    _progressMap.update { it + (video.id to pos) }
                    if (dur > 0L) _durationMap.update { it + (video.id to dur) }
                }
                delay(PROGRESS_POLL_INTERVAL_MS)
            }
        }
    }

    fun loadVideosFromApi(count: Int = 12, apiUrl: String? = null) {
        currentApiUrl = apiUrl
        _videosState.value = VideosUiState.Loading
        viewModelScope.launch {
            runCatching {
                VideoRepository.fetchRandomVideos(count = count, fallbackOnError = true, apiUrl = apiUrl)
            }.onSuccess { list ->
                if (list.isEmpty()) {
                    _videosState.value = VideosUiState.Error("接口未返回视频，请稍后重试")
                    return@onSuccess
                }
                _videosState.value = VideosUiState.Success(videos = list)
                if (playerCenter != null) loadInitialVideos(list)
            }.onFailure { t ->
                _videosState.value = VideosUiState.Error("加载失败: ${t.message ?: "网络异常"}")
            }
        }
    }

    fun loadMoreVideos(count: Int = 8) {
        val current = _videosState.value as? VideosUiState.Success ?: return
        if (current.isLoadingMore || !current.hasMore) return
        _videosState.value = current.copy(isLoadingMore = true)
        viewModelScope.launch {
            runCatching {
                VideoRepository.fetchRandomVideos(count = count, fallbackOnError = false, apiUrl = currentApiUrl)
            }.onSuccess { newVideos ->
                val merged = current.videos + newVideos.filter { nv ->
                    current.videos.none { it.videoUrl == nv.videoUrl }
                }
                _videosState.value = current.copy(
                    videos = merged,
                    isLoadingMore = false,
                    hasMore = newVideos.isNotEmpty()
                )
                startProgressLoop()
            }.onFailure {
                // 失败不关闭分页，允许用户重试
                _videosState.value = current.copy(isLoadingMore = false, hasMore = true)
            }
        }
    }

    fun checkAndLoadMore(currentIndex: Int, threshold: Int = 3) {
        val state = _videosState.value as? VideosUiState.Success ?: return
        val remaining = state.videos.size - currentIndex - 1
        if (remaining <= threshold && !state.isLoadingMore && state.hasMore) {
            loadMoreVideos()
        }
    }

    fun consumeAutoPlayScrollEvent() { _autoPlayScrollEvent.value = null }

    fun onResume() { if (_isPlaying.value) play() }
    fun onPause() { pause() }

    fun copyVideoUrl(context: Context, videoUrl: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("视频链接", videoUrl))
    }

    // ===== 缓存管理 =====
    private val _cacheSize = MutableStateFlow(0L)
    val cacheSize: StateFlow<Long> = _cacheSize.asStateFlow()

    fun refreshCacheSize(context: Context) {
        viewModelScope.launch {
            val size = withContext(Dispatchers.IO) {
                var total = 0L
                val cacheDir = File(context.cacheDir, "video_cache")
                if (cacheDir.exists()) total += dirSize(cacheDir)
                context.cacheDir.listFiles()?.forEach { f ->
                    if (f.name.startsWith("dl_") && f.isFile) total += f.length()
                }
                total
            }
            _cacheSize.value = size
        }
    }

    fun clearCache(context: Context, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // 不释放 SimpleCache 实例，只清除缓存内容，避免 player 持有已释放引用崩溃
                    val cache = simpleCache
                    if (cache != null) {
                        cache.keys.toList().forEach { key ->
                            runCatching { cache.removeResource(key) }
                        }
                    }
                    // 删除临时下载文件
                    context.cacheDir.listFiles()?.forEach { f ->
                        if (f.name.startsWith("dl_") && f.isFile) f.delete()
                    }
                }
                _cacheSize.value = 0L
                withContext(Dispatchers.Main) { onResult(true, "缓存已清空") }
            } catch (e: Exception) {
                Log.e(TAG, "清空缓存失败", e)
                withContext(Dispatchers.Main) { onResult(false, "清空失败: ${e.message}") }
            }
        }
    }

    private fun dirSize(dir: File): Long {
        if (!dir.exists()) return 0
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    private fun formatSize(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        val kb = bytes / 1024.0
        return when {
            mb >= 1 -> "%.1f MB".format(mb)
            kb >= 1 -> "%.0f KB".format(kb)
            else -> "$bytes B"
        }
    }

    val cacheSizeText: StateFlow<String> = MutableStateFlow("0 B").also { derived ->
        viewModelScope.launch {
            _cacheSize.collect { derived.value = formatSize(it) }
        }
    }.asStateFlow()

    // ===== 下载视频到本地 =====
    private val _downloadState = MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())
    val downloadState: StateFlow<Map<String, DownloadStatus>> = _downloadState.asStateFlow()

    fun downloadVideo(context: Context, videoId: String, videoUrl: String) {
        val appCtx = context.applicationContext
        if (_downloadState.value[videoId] == DownloadStatus.Downloaded) {
            sendMessage("已下载过该视频")
            return
        }
        if (_downloadState.value[videoId] == DownloadStatus.Downloading) return
        _downloadState.update { it + (videoId to DownloadStatus.Downloading) }
        sendMessage("开始下载...")

        viewModelScope.launch {
            var success = false
            var errorMsg: String? = null
            try {
                val savedPath = withContext(Dispatchers.IO) {
                    // sanitize 文件名，防止路径穿越
                    val safeId = videoId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                    val fileName = "video_${safeId}.mp4"
                    val tmpFile = File(appCtx.cacheDir, "dl_$fileName")

                    // HttpURLConnection + 手动重定向（instanceFollowRedirects=false）
                    var url: URL? = URL(videoUrl)
                    var redirectCount = 0
                    var conn: java.net.HttpURLConnection? = null
                    try {
                        while (url != null && redirectCount < 5) {
                            conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                                instanceFollowRedirects = false
                                connectTimeout = 15000
                                readTimeout = 30000
                                requestMethod = "GET"
                                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36")
                                connect()
                            }
                            val code = conn.responseCode
                            when {
                                code in 300..399 -> {
                                    val location = conn.getHeaderField("Location")
                                    conn.disconnect()
                                    if (location.isNullOrBlank()) throw java.io.IOException("重定向但无Location")
                                    url = URL(url, location)
                                    redirectCount++
                                }
                                code != 200 -> throw java.io.IOException("HTTP $code")
                                else -> break
                            }
                        }
                        if (conn == null || conn.responseCode != 200) throw java.io.IOException("连接失败")
                        conn.inputStream.use { input ->
                            FileOutputStream(tmpFile).use { output -> input.copyTo(output, bufferSize = 8192) }
                        }
                        conn.disconnect()
                    } finally {
                        conn?.disconnect()
                    }

                    if (!tmpFile.exists() || tmpFile.length() == 0L) throw java.io.IOException("临时文件为空")

                    val finalPath: String
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val values = android.content.ContentValues().apply {
                            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        }
                        val collection = android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI
                        val uri = appCtx.contentResolver.insert(collection, values)
                            ?: throw java.io.IOException("MediaStore插入失败")
                        appCtx.contentResolver.openOutputStream(uri).use { output ->
                            tmpFile.inputStream().use { input -> input.copyTo(output!!, bufferSize = 8192) }
                        }
                        finalPath = "Download/$fileName"
                    } else {
                        val downloadDir = File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                            "immersive_video"
                        ).apply { mkdirs() }
                        val outputFile = File(downloadDir, fileName)
                        tmpFile.copyTo(outputFile, overwrite = true)
                        finalPath = outputFile.absolutePath
                    }
                    tmpFile.delete()
                    finalPath
                }
                success = true
                _downloadState.update { it + (videoId to DownloadStatus.Downloaded) }
                sendMessage("已保存到 $savedPath")
            } catch (e: Exception) {
                errorMsg = e.message ?: e.javaClass.simpleName
                Log.e(TAG, "下载失败: $errorMsg", e)
                _downloadState.update { it - videoId }
                sendMessage("下载失败: ${errorMsg ?: "未知错误"}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        progressJob?.cancel()
        doubleTapJob?.cancel()
        listOfNotNull(playerTop, playerCenter, playerBottom).forEach { p ->
            runCatching { p.stop() }
            runCatching { p.release() }
        }
        playerTop = null; playerCenter = null; playerBottom = null
        runCatching { simpleCache?.release() }
        simpleCache = null
        cacheDataSourceFactory = null
        httpFactory = null
        mediaSourceFactory = null
    }
}
