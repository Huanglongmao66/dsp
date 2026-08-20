package com.dsp.immersiveshortvideo.data

import com.dsp.immersiveshortvideo.model.ShortVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * 视频数据仓库
 * 默认接口：https://api.yujn.cn/api/zzxjj.php?type=video
 * 支持多种API格式：
 *   - 302重定向：直接获取视频URL
 *   - JSON格式：{"video_url":"..."} 或 {"data":"..."} 或 {"url":"..."} 或 {"success":true,"video_url":"..."}
 *   - 纯文本URL
 */
object VideoRepository {

    private const val DEFAULT_API_URL = "https://api.yujn.cn/api/zzxjj.php?type=json"

    /** OkHttp 客户端：15秒超时 + 自动跟随重定向 */
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    /**
     * 检测接口是否可用
     * @return Pair(是否可用, 视频URL或错误信息)
     */
    suspend fun checkApiAvailability(apiUrl: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(apiUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .header("Accept", "*/*")
                .get()
                .build()
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string()?.trim() ?: ""
                if (body.isNotEmpty()) {
                    val videoUrl = parseVideoUrl(body)
                    if (videoUrl != null) {
                        Pair(true, videoUrl)
                    } else {
                        // 可能是302重定向，检查最终URL
                        val finalUrl = response.request.url.toString()
                        if (finalUrl.startsWith("http") && (finalUrl.contains(".mp4") || finalUrl.contains(".m3u8") || finalUrl.contains("video"))) {
                            Pair(true, finalUrl)
                        } else {
                            Pair(false, "无法解析视频地址")
                        }
                    }
                } else {
                    Pair(false, "接口返回为空")
                }
            }
        } catch (e: Exception) {
            Pair(false, e.message ?: "网络异常")
        }
    }

    /**
     * 拉取 count 条随机短视频
     */
    suspend fun fetchRandomVideos(
        count: Int = 12,
        fallbackOnError: Boolean = true,
        apiUrl: String? = null
    ): List<ShortVideo> = withContext(Dispatchers.IO) {
        val result = ArrayList<ShortVideo>(count)
        val urlsSeen = HashSet<String>()
        repeat(count) { index ->
            try {
                val videoUrl = requestOneVideoUrl(apiUrl)
                if (videoUrl != null && videoUrl !in urlsSeen) {
                    urlsSeen.add(videoUrl)
                    result.add(buildShortVideo(index.toString(), videoUrl, index))
                } else if (fallbackOnError) {
                    result.add(MockVideoSource.getDemoVideos()[index % MockVideoSource.getDemoVideos().size])
                }
            } catch (t: Throwable) {
                if (fallbackOnError) {
                    result.add(MockVideoSource.getDemoVideos()[index % MockVideoSource.getDemoVideos().size])
                }
            }
        }
        result
    }

    /**
     * 发起单次HTTP请求，兼容多种API格式解析视频URL
     */
    private fun requestOneVideoUrl(customUrl: String? = null): String? {
        val url = customUrl ?: DEFAULT_API_URL
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
            .header("Accept", "*/*")
            .get()
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string()?.trim() ?: return null
            // 先尝试解析body
            val parsedUrl = parseVideoUrl(body)
            if (parsedUrl != null) {
                return parsedUrl
            }
            // 如果body解析失败，检查最终URL（302重定向的情况）
            val finalUrl = response.request.url.toString()
            if (isVideoUrl(finalUrl)) {
                return finalUrl
            }
            return null
        }
    }

    /**
     * 判断是否为视频URL
     */
    private fun isVideoUrl(url: String): Boolean {
        val videoExtensions = listOf(".mp4", ".m3u8", ".webm", ".mov", ".3gp")
        val videoKeywords = listOf("video", "mp4", "play", "stream", "live")
        return videoExtensions.any { url.contains(it) } || 
               videoKeywords.any { url.contains(it, ignoreCase = true) }
    }

    /**
     * 智能解析视频URL，支持多种格式
     */
    private fun parseVideoUrl(body: String): String? {
        // 1. 如果body本身就是URL
        if (body.startsWith("http") && isVideoUrl(body)) {
            return body
        }
        
        // 2. 尝试解析JSON
        return try {
            val json = JSONObject(body)
            // 常见字段名依次尝试
            listOf("video_url", "url", "data", "video", "link", "play_url").forEach { key ->
                val value = json.optString(key, "").trim()
                if (value.isNotEmpty() && value.startsWith("http")) {
                    return@parseVideoUrl value
                }
            }
            // 嵌套JSON（如 data 字段是JSON字符串）
            json.optJSONObject("data")?.let { dataJson ->
                listOf("video_url", "url", "video", "link", "play_url").forEach { key ->
                    val value = dataJson.optString(key, "").trim()
                    if (value.isNotEmpty() && value.startsWith("http")) {
                        return@parseVideoUrl value
                    }
                }
            }
            // 如果有 success 字段但无URL，尝试其他字段
            if (json.optBoolean("success", false)) {
                // 尝试所有可能的URL字段
                for (key in json.keys()) {
                    val value = json.optString(key, "").trim()
                    if (value.startsWith("http")) {
                        return@parseVideoUrl value
                    }
                }
            }
            null
        } catch (e: Exception) {
            // 3. 非JSON，直接检查是否为URL
            if (body.startsWith("http")) body else null
        }
    }

    // ========== 随机生成辅助 ==========
    private val randomAuthors = listOf(
        "街头摄影师阿K", "美食探店狂魔", "旅行日记Luna", "健身教练Max",
        "猫咪罐头", "萌宠日常", "音乐制作人Leo", "生活记录者",
        "科技测评小王", "校园日常", "舞蹈练习生", "手工达人",
        "剧情小剧场", "乡村生活", "城市夜景", "穿搭日记"
    )
    private val randomDescs = listOf(
        "今天也要元气满满呀✨ #日常 #治愈",
        "发现一家宝藏小店，味道绝了！#美食探店 #吃货",
        "人生就是一场说走就走的旅行🌍 #旅行 #风景",
        "坚持打卡第100天，一起变更好💪 #健身 #自律",
        "它怎么可以这么可爱！！🐱 #萌宠 #猫咪",
        "这首歌单曲循环了一整天🎵 #音乐分享",
        "这一期的新品开箱太惊喜了📦 #开箱 #好物推荐",
        "周末就是要和朋友一起嗨🎉 #生活记录",
        "学会这个技巧，效率翻倍 #干货分享 #学习",
        "街头偶遇的一幕，被感动到了🥹 #人间温暖"
    )
    private val randomTagsList = listOf(
        listOf("日常", "治愈"),
        listOf("美食", "探店"),
        listOf("旅行", "风景"),
        listOf("健身", "自律"),
        listOf("萌宠", "云吸猫"),
        listOf("音乐", "热歌榜"),
        listOf("穿搭", "OOTD"),
        listOf("剧情", "反转")
    )
    private val randomMusics = listOf(
        "起风了" to "买辣椒也用券",
        "稻香" to "周杰伦",
        "光年之外" to "邓紫棋",
        "Blinding Lights" to "The Weeknd",
        "夜曲" to "周杰伦",
        "Shape of You" to "Ed Sheeran",
        "晴天" to "周杰伦",
        "漠河舞厅" to "柳爽",
        "See You Again" to "Wiz Khalifa",
        "海阔天空" to "Beyond"
    )

    /** 根据视频URL生成唯一索引，构建完整的ShortVideo对象 */
    private fun buildShortVideo(id: String, videoUrl: String, idx: Int): ShortVideo {
        val seed = "dy_${idx}_${videoUrl.hashCode().toUInt()}"
        val author = randomAuthors[idx % randomAuthors.size]
        val desc = randomDescs[idx % randomDescs.size]
        val tags = randomTagsList[idx % randomTagsList.size]
        val (musicName, musicAuthor) = randomMusics[idx % randomMusics.size]
        val avSeed = (idx % 70) + 1
        return ShortVideo(
            id = "dy_${id}_${videoUrl.hashCode().toUInt()}",
            videoUrl = videoUrl,
            coverUrl = "https://picsum.photos/seed/${seed}_cover/720/1280",
            description = desc,
            authorName = author,
            authorAvatar = "https://i.pravatar.cc/150?img=$avSeed",
            isFollowed = Random.Default.nextBoolean(),
            likeCount = formatCount(Random.Default.nextLong(1_000, 5_000_000)),
            commentCount = formatCount(Random.Default.nextLong(500, 300_000)),
            shareCount = formatCount(Random.Default.nextLong(100, 200_000)),
            collectCount = formatCount(Random.Default.nextLong(200, 500_000)),
            musicName = musicName,
            musicAuthor = musicAuthor,
            musicCover = "https://picsum.photos/seed/${seed}_music/200/200",
            durationMs = 15000L,
            tags = tags
        )
    }

    /** 数字格式化：12345 → "1.2w" */
    private fun formatCount(n: Long): String = when {
        n >= 10000 -> {
            val wan = n / 10000.0
            "%.1fw".format(wan)
        }
        n >= 1000 -> "%.1fk".format(n / 1000.0)
        else -> n.toString()
    }
}
