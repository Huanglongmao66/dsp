package com.dsp.immersiveshortvideo

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.dsp.immersiveshortvideo.player.PlayerViewModel
import com.dsp.immersiveshortvideo.ui.screens.ShortVideoScreen
import kotlin.system.exitProcess

/**
 * App入口Activity
 * 核心任务：设置沉浸式全屏（刘海屏/挖孔屏兼容）、加载主Screen、双击返回退出
 */
class MainActivity : ComponentActivity() {

    private lateinit var playerViewModel: PlayerViewModel

    // 双击返回退出：2秒内按两次返回才退出
    private var lastBackPressTime = 0L
    private val exitInterval = 2000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ====== 沉浸式全屏 + 刘海屏兼容（setDecorFitsSystemWindows必须在setContentView之前）======
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 保持屏幕常亮（看视频时自动休眠很烦）
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 允许内容延伸到挖孔/刘海区域
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        // insetsController / 旧版 flags 必须在 decorView attach 后才能安全调用
        window.decorView.post {
            try {
                // Android 11+：使用WindowInsetsController进入沉浸式
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    window.insetsController?.let { controller ->
                        controller.hide(WindowInsets.Type.systemBars())
                        controller.systemBarsBehavior =
                            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                } else {
                    // 兼容旧版本：使用沉浸式Sticky标志
                    @Suppress("DEPRECATION")
                    window.decorView.systemUiVisibility = (
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                        )
                }
            } catch (_: Throwable) { /* 沉浸式失败不影响App主体 */ }
        }

        // 初始化ViewModel（使用同一个Provider保证全局单例）
        playerViewModel = ViewModelProvider(this)[PlayerViewModel::class.java]

        setContent {
            ImmersiveShortVideoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    ShortVideoScreen(viewModel = playerViewModel)
                }
            }
        }
    }

    /**
     * 拦截返回键：双击2次（间隔2秒内）才真正退出App到桌面
     */
    override fun onBackPressed() {
        val now = System.currentTimeMillis()
        if (now - lastBackPressTime <= exitInterval) {
            // 2秒内第二次按返回，真正退出
            super.onBackPressed()
            finishAffinity()
            exitProcess(0)
        } else {
            // 第一次按返回，提示用户再按一次
            lastBackPressTime = now
            Toast.makeText(this, "再按一次返回键退出应用", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        playerViewModel.onResume()
    }

    override fun onPause() {
        super.onPause()
        playerViewModel.onPause()
    }
}

/**
 * 应用主题：纯暗色沉浸式主题（短视频App普遍采用黑色背景）
 */
@Composable
fun ImmersiveShortVideoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = darkColorScheme(
        primary = Color(0xFFFE2C55),   // 抖音红作为主色
        secondary = Color(0xFF25F4EE), // 抖音青
        background = Color.Black,
        surface = Color.Black,
        onPrimary = Color.White,
        onSecondary = Color.Black,
        onBackground = Color.White,
        onSurface = Color.White
    )
    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
