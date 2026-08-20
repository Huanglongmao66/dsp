package com.dsp.immersiveshortvideo.data

/**
 * API图标常量（20个可选）
 */
object ApiIcons {
    val icons = listOf(
        "smile", "heart", "star", "thumb", "gem",
        "sparkle", "fire", "bolt", "target", "flower",
        "moon", "sun", "crown", "diamond", "rocket",
        "globe", "shield", "compass", "trophy", "wave"
    )
    
    val iconNames = mapOf(
        "smile" to "笑脸", "heart" to "爱心", "star" to "星星", "thumb" to "点赞", "gem" to "宝石",
        "sparkle" to "闪光", "fire" to "火焰", "bolt" to "闪电", "target" to "靶心", "flower" to "花朵",
        "moon" to "月亮", "sun" to "太阳", "crown" to "皇冠", "diamond" to "钻石", "rocket" to "火箭",
        "globe" to "地球", "shield" to "盾牌", "compass" to "指南针", "trophy" to "奖杯", "wave" to "波浪"
    )
}

/**
 * API配置数据类
 */
data class ApiConfig(
    val id: String,
    val name: String,
    val url: String,
    val icon: String = "smile",
    val isEnabled: Boolean = true
)
