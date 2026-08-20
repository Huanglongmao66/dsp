package com.dsp.immersiveshortvideo.model

/**
 * 短视频数据模型
 * 包含视频播放地址、封面、作者信息、互动数据等完整字段
 */
data class ShortVideo(
    /** 视频唯一ID */
    val id: String,
    /** 视频播放URL（支持HTTPS网络地址或本地asset） */
    val videoUrl: String,
    /** 封面图URL（视频加载前的占位图） */
    val coverUrl: String,
    /** 视频标题/描述文字 */
    val description: String,
    /** 作者昵称 */
    val authorName: String,
    /** 作者头像URL */
    val authorAvatar: String,
    /** 是否关注该作者 */
    val isFollowed: Boolean = false,
    /** 点赞数（人类可读格式，如 "128.5w"） */
    val likeCount: String,
    /** 评论数 */
    val commentCount: String,
    /** 分享数 */
    val shareCount: String,
    /** 收藏数 */
    val collectCount: String,
    /** 背景音乐名 */
    val musicName: String,
    /** 背景音乐作者 */
    val musicAuthor: String,
    /** 背景音乐封面图（用于旋转光盘效果） */
    val musicCover: String,
    /** 视频时长（毫秒，用于进度条） */
    val durationMs: Long = 15000L,
    /** 话题标签列表，如 #美食 #旅行 */
    val tags: List<String> = emptyList()
)
