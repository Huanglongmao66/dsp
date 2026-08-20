package com.dsp.immersiveshortvideo.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * API配置管理器（持久化到SharedPreferences）
 */
class ApiConfigManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("api_configs", Context.MODE_PRIVATE)
    
    // 配置版本号，更新时自动迁移
    private val currentVersion = 6
    private val versionKey = "config_version"

    // 按 URL 去重后的默认配置（保持优先级顺序：默认推荐 > 用户指定的 mp4/yujn/suyanw 新接口）
    private val defaultConfigs = dedupeConfigs(
        listOf(
            // ===== 默认推荐接口（置顶） =====
            ApiConfig("yujn_default", "推荐", "https://api.yujn.cn/api/zzxjj.php?type=json", "star"),

            // ===== 用户添加的 qzqi type=mp4 视频直链（优先级最高） =====
            ApiConfig("qz_dy_random_mp4", "抖音随机", "https://api.qzqi.com/api/v1/DyRandomVideo?type=mp4", "rocket"),
            ApiConfig("qz_qingcun_mp4", "清纯", "https://api.qzqi.com/api/v1/Randclip?type=mp4&id=QingCun", "smile"),
            ApiConfig("qz_nvda_mp4", "女大", "https://api.qzqi.com/api/v1/Randclip?type=mp4&id=NvDa", "heart"),
            ApiConfig("qz_tianmei_mp4", "甜妹", "https://api.qzqi.com/api/v1/Randclip?type=mp4&id=TianMei", "flower"),
            ApiConfig("qz_luoli_mp4", "萝莉", "https://api.qzqi.com/api/v1/Randclip?type=mp4&id=LuoLi", "smile"),
            ApiConfig("qz_bianzhuang_mp4", "变装", "https://api.qzqi.com/api/v1/Randclip?type=mp4&id=BianZhuang", "sparkle"),
            ApiConfig("qz_heisi_mp4", "黑丝", "https://api.qzqi.com/api/v1/Randclip?type=mp4&id=HeiSi", "bolt"),
            ApiConfig("qz_hanfu_mp4", "汉服", "https://api.qzqi.com/api/v1/Randclip?type=mp4&id=HanFu", "crown"),
            ApiConfig("qz_chuanda_mp4", "穿搭", "https://api.qzqi.com/api/v1/Randclip?type=mp4&id=ChuanDa", "sparkle"),
            ApiConfig("qz_xjj_mp4", "小姐姐", "https://api.qzqi.com/api/v1/Randclip?type=mp4&id=GaoZhiLiangXiaoJieJie", "heart"),
            ApiConfig("qz_baisi_mp4", "白丝", "https://api.qzqi.com/api/v1/Randclip?type=mp4&id=BaiSi", "gem"),
            ApiConfig("qz_rewu_mp4", "热舞", "https://api.qzqi.com/api/v1/Randclip?type=mp4&id=ReWu", "fire"),
            ApiConfig("qz_jk_mp4", "JK", "https://api.qzqi.com/api/v1/Randclip?type=mp4&id=jk", "gem"),

            // ===== 用户添加的 yujn.cn API =====
            ApiConfig("yujn_ks", "快手小姐姐", "http://api.yujn.cn/api/ksxjjsp.php", "heart"),
            ApiConfig("yujn_luoli", "萝莉", "http://api.yujn.cn/api/luoli.php?type=video", "smile"),
            ApiConfig("yujn_xjj", "小姐姐", "https://api.yujn.cn/api/xjj.php?type=video", "heart"),
            ApiConfig("yujn_tianmei", "甜妹", "http://api.yujn.cn/api/tianmei.php?type=video", "flower"),
            ApiConfig("yujn_heisi", "黑丝", "http://api.yujn.cn/api/heisis.php?type=video", "bolt"),
            ApiConfig("yujn_baisi", "白丝", "http://api.yujn.cn/api/baisis.php?type=video", "gem"),
            ApiConfig("yujn_bianzhuang", "变装", "http://api.yujn.cn/api/bianzhuang.php", "sparkle"),
            ApiConfig("yujn_rewu", "热舞", "http://api.yujn.cn/api/rewu.php?type=video", "fire"),
            ApiConfig("yujn_xgg", "小哥哥", "http://api.yujn.cn/api/xgg.php?type=video", "smile"),
            ApiConfig("yujn_zzxjj", "小姐姐推荐", "https://api.yujn.cn/api/zzxjj.php?type=video", "star"),

            // ===== 苏颜舞（suyanw）API =====
            ApiConfig("sy_jksp", "JK视频", "https://api.suyanw.cn/api/jksp.php", "gem"),
            ApiConfig("sy_jk", "JK", "https://api.suyanw.cn/api/jhsp.php?msg=JK系列", "gem"),
            ApiConfig("sy_slow", "慢摇", "https://api.suyanw.cn/api/jhsp.php?msg=慢摇系列", "fire"),
            ApiConfig("sy_jiaoman", "小蛮腰", "https://api.suyanw.cn/api/jhsp.php?msg=小蛮腰系列", "sparkle"),
            ApiConfig("sy_whitesilk", "白丝", "https://api.suyanw.cn/api/jhsp.php?msg=白丝系列", "gem"),
            ApiConfig("sy_blacksilk", "黑丝", "https://api.suyanw.cn/api/jhsp.php?msg=黑丝系列", "bolt"),
            ApiConfig("sy_cos", "COS", "https://api.suyanw.cn/api/jhsp.php?msg=COS系列", "target"),
            ApiConfig("sy_female", "女高", "https://api.suyanw.cn/api/jhsp.php?msg=女高系列", "flower"),
            ApiConfig("sy_hot", "热舞", "https://api.suyanw.cn/api/jhsp.php?msg=热舞系列", "fire"),
            ApiConfig("sy_outfit", "穿搭", "https://api.suyanw.cn/api/jhsp.php?msg=穿搭系列", "sparkle"),
            ApiConfig("sy_dress", "变装", "https://api.suyanw.cn/api/jhsp.php?msg=变装系列", "crown"),
            ApiConfig("sy_beauty", "美身材", "https://api.suyanw.cn/api/jhsp.php?msg=美身材系列", "heart"),

            // ===== qzqi Randclip type=json 备用接口（放在后面避免与 mp4 版同名冲突） =====
            ApiConfig("qz_yumeng", "玉梦", "https://api.qzqi.com/api/v1/Randclip?id=YuMeng&type=json", "sparkle"),
            ApiConfig("qz_nvgao", "女高", "https://api.qzqi.com/api/v1/Randclip?id=NvGao&type=json", "flower"),
            ApiConfig("qz_yuzu", "足浴", "https://api.qzqi.com/api/v1/Randclip?id=YuZu&type=json", "bolt"),
            ApiConfig("qz_shejie", "蛇姐", "https://api.qzqi.com/api/v1/Randclip?id=SheJie&type=json", "target"),
            ApiConfig("qz_gaozhiliang", "高质量", "https://api.qzqi.com/api/v1/Randclip?id=GaoZhiLiang&type=json", "star"),
            ApiConfig("qz_xiaojiejie", "小姐姐", "https://api.qzqi.com/api/v1/Randclip?id=XiaoJieJie&type=json", "heart")
        )
    )

    /**
     * 接口去重：
     * - URL 完全相同：保留第一个（按优先级顺序取最前面的）
     * - name 完全相同：保留第一个，第二个开始追加 "(备用)" / "(备用2)" 后缀避免UI覆盖
     */
    private fun dedupeConfigs(list: List<ApiConfig>): List<ApiConfig> {
        val seenUrls = LinkedHashSet<String>()
        val nameCounter = LinkedHashMap<String, Int>()
        val result = mutableListOf<ApiConfig>()

        for (cfg in list) {
            // URL 去重：跳过完全相同的URL
            val urlKey = cfg.url.trimEnd('/').lowercase()
            if (!seenUrls.add(urlKey)) continue

            // name 去重：同名追加 "(备用)" 后缀
            val baseName = cfg.name
            val count = nameCounter.getOrDefault(baseName, 0)
            val finalName = if (count == 0) baseName else "$baseName(备用$count)"
            nameCounter[baseName] = count + 1

            result.add(cfg.copy(name = finalName))
        }
        return result
    }

    fun loadConfigs(): List<ApiConfig> {
        // 检查版本号，版本不匹配时用新的默认配置
        val savedVersion = prefs.getInt(versionKey, 0)
        if (savedVersion != currentVersion) {
            saveConfigs(defaultConfigs)
            prefs.edit().putInt(versionKey, currentVersion).apply()
            return defaultConfigs
        }

        val json = prefs.getString("configs", null)
        if (json == null) {
            saveConfigs(defaultConfigs)
            return defaultConfigs
        }
        val loaded = try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                ApiConfig(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    url = obj.getString("url"),
                    icon = obj.optString("icon", "smile"),
                    isEnabled = obj.optBoolean("enabled", true)
                )
            }
        } catch (e: Exception) {
            defaultConfigs
        }
        // 加载时也做去重，避免旧版本数据/用户手动添加产生重复
        return dedupeConfigs(loaded).ifEmpty { defaultConfigs }
    }

    fun saveConfigs(configs: List<ApiConfig>) {
        val array = JSONArray()
        configs.forEach { config ->
            array.put(JSONObject().apply {
                put("id", config.id)
                put("name", config.name)
                put("url", config.url)
                put("icon", config.icon)
                put("enabled", config.isEnabled)
            })
        }
        prefs.edit().putString("configs", array.toString()).apply()
    }

    fun addConfig(name: String, url: String, icon: String = "smile") {
        val configs = loadConfigs().toMutableList()
        val newId = "custom_${System.currentTimeMillis()}"
        configs.add(ApiConfig(newId, name, url, icon, true))
        saveConfigs(configs)
    }

    fun updateConfig(id: String, name: String, url: String, icon: String) {
        val configs = loadConfigs().map { config ->
            if (config.id == id) config.copy(name = name, url = url, icon = icon) else config
        }
        saveConfigs(configs)
    }

    fun deleteConfig(id: String) {
        val configs = loadConfigs().filter { it.id != id }
        saveConfigs(configs)
    }

    fun getEnabledConfigs(): List<ApiConfig> {
        return loadConfigs().filter { it.isEnabled }
    }

    fun getConfigByName(name: String): ApiConfig? {
        return loadConfigs().find { it.name == name }
    }

    fun setConfigEnabled(id: String, enabled: Boolean) {
        val configs = loadConfigs().map { config ->
            if (config.id == id) config.copy(isEnabled = enabled) else config
        }
        saveConfigs(configs)
    }

    fun setAllConfigsEnabled(results: Map<String, Boolean>) {
        val configs = loadConfigs().map { config ->
            config.copy(isEnabled = results[config.id] ?: true)
        }
        saveConfigs(configs)
    }
}
