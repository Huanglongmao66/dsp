package com.dsp.immersiveshortvideo.data

import com.dsp.immersiveshortvideo.model.ShortVideo

/**
 * 模拟短视频数据源
 * 使用Google官方公开的大体积测试视频地址（可直接播放，无需鉴权）
 * 实际项目中替换为后端接口返回的数据即可
 */
object MockVideoSource {

    /**
     * 获取示例短视频列表
     * 视频源使用Google提供的公开MP4测试地址，兼容性最好
     */
    fun getDemoVideos(): List<ShortVideo> = listOf(
        ShortVideo(
            id = "v001",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            coverUrl = "https://picsum.photos/seed/bunny/720/1280",
            description = "大自然中最可爱的大兔子🐰 每天看一遍，烦恼全忘掉 #治愈系 #萌宠",
            authorName = "森林小精灵",
            authorAvatar = "https://i.pravatar.cc/150?img=32",
            likeCount = "128.5w",
            commentCount = "3.2w",
            shareCount = "1.5w",
            collectCount = "8.6w",
            musicName = "森林圆舞曲",
            musicAuthor = "原声-森林小精灵",
            musicCover = "https://picsum.photos/seed/music1/200/200",
            tags = listOf("治愈系", "萌宠")
        ),
        ShortVideo(
            id = "v002",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            coverUrl = "https://picsum.photos/seed/dream/720/1280",
            description = "当梦境照进现实✨ 这个特效我给满分！你们觉得怎么样？#创意特效",
            authorName = "造梦师Leo",
            authorAvatar = "https://i.pravatar.cc/150?img=12",
            isFollowed = true,
            likeCount = "256.8w",
            commentCount = "15.3w",
            shareCount = "9.2w",
            collectCount = "42.1w",
            musicName = "梦中的婚礼",
            musicAuthor = "钢琴版-理查德",
            musicCover = "https://picsum.photos/seed/music2/200/200",
            tags = listOf("创意特效", "视觉盛宴")
        ),
        ShortVideo(
            id = "v003",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            coverUrl = "https://picsum.photos/seed/fire/720/1280",
            description = "🔥烈焰挑战！这个冬天不怕冷了，一起燃烧卡路里！#健身日常 #挑战",
            authorName = "健身狂人Max",
            authorAvatar = "https://i.pravatar.cc/150?img=68",
            likeCount = "89.2w",
            commentCount = "1.8w",
            shareCount = "6.5k",
            collectCount = "5.3w",
            musicName = "Rock You",
            musicAuthor = "Queen-经典摇滚",
            musicCover = "https://picsum.photos/seed/music3/200/200",
            tags = listOf("健身日常", "挑战")
        ),
        ShortVideo(
            id = "v004",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
            coverUrl = "https://picsum.photos/seed/escape/720/1280",
            description = "说走就走的旅行🌍 逃离城市喧嚣，去看世界的尽头 #旅行vlog #治愈",
            authorName = "背包客小雅",
            authorAvatar = "https://i.pravatar.cc/150?img=47",
            likeCount = "312.4w",
            commentCount = "22.7w",
            shareCount = "18.9w",
            collectCount = "67.3w",
            musicName = "起风了",
            musicAuthor = "买辣椒也用券",
            musicCover = "https://picsum.photos/seed/music4/200/200",
            tags = listOf("旅行vlog", "治愈")
        ),
        ShortVideo(
            id = "v005",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
            coverUrl = "https://picsum.photos/seed/fun/720/1280",
            description = "今天也是开心的一天～🎉 生活就是要及时行乐呀 #日常vlog #快乐",
            authorName = "快乐星球",
            authorAvatar = "https://i.pravatar.cc/150?img=23",
            likeCount = "67.9w",
            commentCount = "8.2k",
            shareCount = "2.1w",
            collectCount = "3.4w",
            musicName = "快乐崇拜",
            musicAuthor = "潘玮柏/张韶涵",
            musicCover = "https://picsum.photos/seed/music5/200/200",
            tags = listOf("日常vlog", "快乐")
        ),
        ShortVideo(
            id = "v006",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4",
            coverUrl = "https://picsum.photos/seed/joyride/720/1280",
            description = "深夜飙车党的快乐🏎️ 无人的街道，自由的灵魂！#汽车 #速度与激情",
            authorName = "车神Ken",
            authorAvatar = "https://i.pravatar.cc/150?img=5",
            likeCount = "178.6w",
            commentCount = "5.4w",
            shareCount = "7.2w",
            collectCount = "19.8w",
            musicName = "See You Again",
            musicAuthor = "Wiz Khalifa",
            musicCover = "https://picsum.photos/seed/music6/200/200",
            tags = listOf("汽车", "速度与激情")
        ),
        ShortVideo(
            id = "v007",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
            coverUrl = "https://picsum.photos/seed/melt/720/1280",
            description = "夏天最治愈的声音❄️ 冰雪融化，心静自然凉 #ASMR #解压",
            authorName = "解压小站",
            authorAvatar = "https://i.pravatar.cc/150?img=44",
            likeCount = "45.3w",
            commentCount = "2.1w",
            shareCount = "8.5k",
            collectCount = "15.6w",
            musicName = "纯音乐-冰雪",
            musicAuthor = "ASMR原创",
            musicCover = "https://picsum.photos/seed/music7/200/200",
            tags = listOf("ASMR", "解压")
        ),
        ShortVideo(
            id = "v008",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
            coverUrl = "https://picsum.photos/seed/sintel/720/1280",
            description = "每一帧都是壁纸级画面🎬 神级动画短片推荐！#动漫 #神作",
            authorName = "动画控",
            authorAvatar = "https://i.pravatar.cc/150?img=16",
            likeCount = "521.7w",
            commentCount = "38.9w",
            shareCount = "29.5w",
            collectCount = "156.2w",
            musicName = "Sintel OST",
            musicAuthor = "Blender Foundation",
            musicCover = "https://picsum.photos/seed/music8/200/200",
            tags = listOf("动漫", "神作")
        )
    )
}
