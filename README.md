# DSP - 沉浸式短视频

一个基于 Jetpack Compose + Media3 ExoPlayer 的沉浸式短视频 Android 应用，支持多接口分类、预加载、手势控制、下载缓存等功能。

## 下载

[![Download APK](https://img.shields.io/badge/Download-v0.0.01-blue)](https://github.com/Huanglongmao66/dsp/releases/download/v0.0.01/dsp-v0.0.01.apk)

- 版本：v0.0.01
- 大小：约 19 MB
- 最低系统：Android 7.0 (API 24)
- 目标系统：Android 14 (API 34)

## 功能特性

### 播放核心
- **3 实例播放器池**：上/中/下三页复用 ExoPlayer，滑动时零缓冲等待
- **磁盘预加载**：SimpleCache 200MB LRU 缓存，相邻页提前 prepare
- **进度条同步**：通过 ExoPlayer 真实 duration 驱动，告别进度条不同步
- **自动播放**：可开关的连播模式，视频结束后自动切换下一条

### 手势交互
- **上下滑动**：切换视频（VerticalPager）
- **点击**：右侧悬浮按钮控制播放/暂停
- **长按**：加速播放（支持 0.25x ~ 3.0x）
- **双击**：点赞动画
- **双击返回退出**：2 秒内按两次返回键才退出应用

### 画面模式
支持 4 种缩放模式，持久化保存选择：
| 模式 | 说明 |
|---|---|
| 默认 | 裁剪铺满全屏（推荐） |
| 等比 | 完整显示，黑边补齐 |
| 铺满 | 裁剪内容，全屏铺满 |
| 自适应 | 自动按比例适配屏幕 |

### 设置面板
底部半屏菜单，包含：
- 4 个快捷图标：复制链接 / 下载 / 纯净模式 / 清空缓存
- 连播开关（Material Switch）
- 播放速度选择（0.25x ~ 3.0x，10 档）
- 缓存管理（显示当前缓存大小，一键清理）
- 画面模式选择（单选对话框）

### 接口管理
- 内置 30+ 视频接口，按分类展示（推荐/清纯/女大/甜妹/萝莉/变装/黑丝/汉服/穿搭/JK 等）
- 支持自定义接口添加、可用性检测
- 接口按 name + URL 自动去重
- 分类选择持久化，重启自动恢复
- API 配置版本迁移，升级自动覆盖旧配置

### 下载与缓存
- 视频下载到公共 Download 目录（支持 Android 10+ MediaStore）
- HttpURLConnection 重定向跟随 + 超时重试
- 下载状态实时显示（下载中/已下载）
- 缓存大小统计与一键清空（不释放 SimpleCache 实例，避免播放器崩溃）

### 沉浸式体验
- 全屏沉浸式（隐藏状态栏和导航栏）
- 纯净模式：隐藏所有 UI 组件，仅保留视频和手势
- 屏幕常亮（WAKE_LOCK）
- 竖屏锁定

## 技术栈

| 分类 | 技术 |
|---|---|
| UI 框架 | Jetpack Compose + Material3 |
| 播放器 | Media3 ExoPlayer 1.2.1 |
| 架构模式 | MVVM（ViewModel + StateFlow） |
| 图片加载 | Coil |
| 网络请求 | OkHttp |
| 异步 | Kotlin Coroutines + Flow |
| 缓存 | SimpleCache (LRU 200MB) |
| 系统适配 | Accompanist SystemUiController + SplashScreen |
| 最低 SDK | Android 7.0 (API 24) |
| 编译 SDK | Android 14 (API 34) |
| JDK | 17 |

## 项目结构

```
app/src/main/java/com/dsp/immersiveshortvideo/
├── MainActivity.kt                 # 入口 Activity，沉浸式全屏 + 双击返回退出
├── data/
│   ├── ApiConfig.kt                # API 配置数据类
│   ├── ApiConfigManager.kt         # API 配置管理（持久化/去重/版本迁移）
│   ├── MockVideoSource.kt          # 本地模拟数据源
│   └── VideoRepository.kt          # 视频数据仓库（网络请求）
├── model/
│   └── ShortVideo.kt              # 视频数据模型
├── player/
│   ├── PlayerViewModel.kt          # 播放器 ViewModel（核心业务逻辑）
│   └── VideoScaleMode.kt           # 画面缩放模式枚举
└── ui/
    ├── components/
    │   ├── Animations.kt           # 飘心动画等
    │   ├── CategoryItem.kt         # 分类列表项
    │   ├── GestureDetector.kt      # 手势检测器
    │   ├── InfoPanels.kt           # 信息面板
    │   ├── RightInteractionPanel.kt# 右侧交互按钮列
    │   ├── VideoPlayer.kt          # 播放器组件（PlayerView + 缩放模式）
    │   └── VideoSettingsSheet.kt   # 底部设置半屏面板
    └── screens/
        ├── ApiSettingsPage.kt      # API 设置页面
        ├── CategoryPage.kt         # 分类选择页面
        └── ShortVideoScreen.kt     # 主界面（VerticalPager + 覆盖层）
```

## 构建

```bash
# 克隆仓库
git clone https://github.com/Huanglongmao66/dsp.git
cd dsp

# 构建 Debug APK
./gradlew assembleDebug

# 构建产物路径
# app/build/outputs/apk/debug/app-debug.apk
```

## GitHub Actions 自动构建

推送 `v*` 格式的 tag 即可触发自动构建并发布 Release：

```bash
git tag v0.0.02
git push origin v0.0.02
```

构建流程：Checkout → JDK 17 → Gradle → assembleDebug → 重命名 APK → 上传到 GitHub Release

## 下载安装

1. 前往 [Releases 页面](https://github.com/Huanglongmao66/dsp/releases)
2. 下载最新版 `dsp-v0.0.01.apk`
3. 手机开启「允许安装未知来源应用」
4. 点击 APK 安装即可

## License

MIT
