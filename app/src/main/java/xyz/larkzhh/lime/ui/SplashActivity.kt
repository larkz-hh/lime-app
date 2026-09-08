package xyz.larkzhh.lime.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.delay
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.ui.theme.LimeTheme
import kotlin.time.Duration.Companion.milliseconds

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LimeTheme {
                SplashContent()
            }
        }
    }
}

@Composable
private fun SplashContent() {
    val context = LocalContext.current
    val animEnabled = remember { AppSplashAnim.enabled() }
    val gotoMain: () -> Unit = {
        context.startActivity(Intent(context, MainActivity::class.java))
        (context as? Activity)?.finish()// 结束启动页
    }
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset("lottie/lime.lottie"))
    var lottieFading by remember { mutableStateOf(false) }// 续播后开始淡出
    var showLogo by remember { mutableStateOf(false) }// 淡出结束后炸出 logo 出现
    var lottieP by remember { mutableFloatStateOf(0f) }// 当前播放进度
    val acceleratingEasing: Easing = remember { Easing { t -> t * t } }// 加速收尾
    LaunchedEffect(composition) {
        if (!animEnabled) return@LaunchedEffect
        val comp = composition ?: return@LaunchedEffect
        val fullMs = comp.duration.coerceAtLeast(16f)
        // 完播
        val playMs = (fullMs * 0.5f).toInt().coerceAtLeast(16)
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(durationMillis = playMs, easing = acceleratingEasing),
        ) { value, _ -> lottieP = value }
        // 续播 18
        lottieP = 0f
        animate(
            initialValue = 0f,
            targetValue = 0.18f,
            animationSpec = tween(durationMillis = (fullMs * 0.18f).toInt().coerceAtLeast(16), easing = LinearEasing),
        ) { value, _ -> lottieP = value }
        lottieFading = true
    }
    val lottieAlpha = remember { Animatable(1f) }
    LaunchedEffect(lottieFading) {
        if (lottieFading) {
            lottieAlpha.animateTo(0f, tween(durationMillis = 150))
        }
    }
    LaunchedEffect(lottieFading) {
        if (lottieFading) {
            showLogo = true
        }
    }
    val logoScale = remember { Animatable(0f) }
    val logoAlpha = remember { Animatable(0f) }
    LaunchedEffect(showLogo) {
        if (showLogo) {
            logoAlpha.animateTo(1f, tween(100))
            logoScale.animateTo(1.25f, tween(100, easing = LinearOutSlowInEasing))
            logoScale.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 260f))
            delay(650.milliseconds)
            gotoMain()
        }
    }
    // 关闭动画静态 app_logo
    LaunchedEffect(Unit) {
        if (!animEnabled) {
            delay(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 100.milliseconds
                else 400.milliseconds
            )
            gotoMain()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        if (animEnabled) {
            if (composition != null) {
                LottieAnimation(
                    composition = composition,
                    progress = { lottieP },
                    modifier = Modifier
                        .size(240.dp)
                        .alpha(lottieAlpha.value),
                )
            }
            if (showLogo) {
                Image(
                    painter = painterResource(R.drawable.app_logo),
                    contentDescription = null,
                    modifier = Modifier
                        .size(200.dp)
                        .graphicsLayer {
                            scaleX = logoScale.value
                            scaleY = logoScale.value
                            alpha = logoAlpha.value
                        },
                )
            }
        } else {
            // 关闭动画，静态展示图标
            Image(
                painter = painterResource(R.drawable.app_logo),
                contentDescription = null,
                modifier = Modifier.size(200.dp),
            )
        }
    }
}
