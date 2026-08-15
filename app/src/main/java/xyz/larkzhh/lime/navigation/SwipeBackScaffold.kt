package xyz.larkzhh.lime.navigation

import android.annotation.SuppressLint
import androidx.activity.BackEventCompat
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

/// 返回提交/取消后的结算冷却时长
private const val BACK_COOLDOWN_MS = 280L

/// 预测性返回的激活阈值
private const val BACK_ACTIVATION = 0.05f

/// 快速轻弹的速度阈值（dp/秒）
private const val FLING_VELOCITY_DP = 320f

/**
 * 通用的可交互滑动导航容器。
 *
 * - 右滑预测性返回
 * - 左滑前进
 * @param backEnabled 是否启用侧滑返回功能
 * @param backThreshold 侧滑返回的触发阈值
 * @param forwardPeek 侧滑前进时的预览内容
 * @param forwardThreshold 侧滑前进的触发阈值
 * @param onCommitForward 侧滑前进的回调函数
 * @param dragSensitivity 拖动阻尼，页面移动 = 手指位移 × 阻尼系数
 * @param tabContentRegion 触摸点是否位于横向滑动区域
 * @param tabAtLeftmost 判断当前 Tab 是否处于最左侧
 * @param revealEntryId 返回栈中要露出的上一页 Entry 的 id
 * @param content 当前页面的实际内容
 */
@SuppressLint("VisibleForTests")
@Composable
fun SwipeBackScaffold(
    backEnabled: Boolean,
    backThreshold: Float = 1f / 3f,
    forwardPeek: (@Composable () -> Unit)? = null,
    forwardThreshold: Float = 1f / 4f,
    onCommitForward: () -> Unit = {},
    dragSensitivity: Float = 0.6f,
    tabContentRegion: ((Offset) -> Boolean)? = null,// null: 整页不是tab内容区
    tabAtLeftmost: () -> Boolean = { true },
    revealEntryId: () -> String? = { null },
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }// 获取当前屏幕的宽度
    val flingVelocityPx = with(density) { FLING_VELOCITY_DP.dp.toPx() }// 轻弹速度阈值
    val scope = rememberCoroutineScope()
    val forwardActive = forwardPeek != null
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val backActive = backEnabled && backDispatcher != null
    val hasTabRegion = tabContentRegion != null
    val offsetX = remember { Animatable(0f) }// 判断左滑方向偏移量

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (backActive || forwardActive) {
                    Modifier.pointerInput(backActive, forwardActive, hasTabRegion) {
                        var gestureIsBack: Boolean? = null// 单次手势的方向状态
                        var backAccumulatedX = 0f// 累计滑动距离
                        var backStarted = false// 本次右滑是否越过激活阈值
                        var backCoolingDown = false// 上一次返回提交、取消后的结算冷却

                        detectLockingHorizontalDrag(
                            interceptAtDown = { pos ->
                                if (!hasTabRegion) {
                                    false
                                } else {
                                    val inTab = tabContentRegion?.invoke(pos) ?: false
                                    !inTab || tabAtLeftmost()
                                }
                            },
                            resolveLock = { isBack, pos ->
                                val canBack = backActive && !backCoolingDown
                                if (!hasTabRegion) {
                                    // 详情页非tab可滑动部分
                                    when {
                                        isBack && canBack -> LockMode.ACT
                                        isBack && backActive -> LockMode.CONSUME
                                        !isBack && forwardActive -> LockMode.ACT
                                        else -> LockMode.RELEASE
                                    }
                                } else {
                                    val inTab = tabContentRegion?.invoke(pos) ?: false
                                    if (inTab) {
                                        // tab内容区最左页右滑触发返回
                                        when {
                                            isBack && tabAtLeftmost() && canBack -> LockMode.ACT
                                            isBack && tabAtLeftmost() && backActive -> LockMode.CONSUME
                                            else -> LockMode.RELEASE
                                        }
                                    } else {
                                        // tab外右滑返回
                                        when {
                                            isBack && canBack -> LockMode.ACT
                                            isBack && backActive -> LockMode.CONSUME
                                            !isBack && forwardActive -> LockMode.ACT
                                            else -> LockMode.CONSUME
                                        }
                                    }
                                }
                            },
                            onLock = { isBack, _ ->
                                gestureIsBack = isBack
                                if (isBack) {
                                    backAccumulatedX = 0f
                                    backStarted = false
                                }
                            },
                            onDrag = { dragAmount, pos ->
                                val damped = dragAmount * dragSensitivity
                                if (gestureIsBack == true) {
                                    // 右滑返回
                                    backAccumulatedX = (backAccumulatedX + damped).coerceAtLeast(0f)
                                    val progress = (backAccumulatedX / screenWidthPx).coerceIn(0f, 1f)
                                    if (!backStarted && progress >= BACK_ACTIVATION) {
                                        // 超过激活阈值，开启预测性返回会话，预览上一页并压暗
                                        backStarted = true
                                        SwipeBackScrimState.revealEntryId = revealEntryId()
                                        backDispatcher?.dispatchOnBackStarted(
                                            BackEventCompat(pos.x, pos.y, 0f, BackEventCompat.EDGE_LEFT)
                                        )
                                    }
                                    if (backStarted) {
                                        SwipeBackScrimState.progress = progress
                                        backDispatcher?.dispatchOnBackProgressed(
                                            BackEventCompat(pos.x, pos.y, progress, BackEventCompat.EDGE_LEFT)
                                        )
                                    }
                                } else {
                                    // 左滑前进
                                    val target = (offsetX.value + damped).coerceIn(-screenWidthPx, 0f)
                                    scope.launch { offsetX.snapTo(target) }
                                }
                            },
                            onDragEnd = { velocityX ->
                                if (gestureIsBack == true) {
                                    val backFling = velocityX >= flingVelocityPx
                                    // 短距离拖动
                                    if (!backStarted) {
                                        if (backFling) backDispatcher?.onBackPressed()// 快速轻弹直接返回
                                        SwipeBackScrimState.revealEntryId = null
                                        SwipeBackScrimState.progress = 0f
                                    } else {
                                        val progress = (backAccumulatedX / screenWidthPx).coerceIn(0f, 1f)
                                        // 结算冷却
                                        backCoolingDown = true
                                        if (progress >= backThreshold || backFling) {
                                            // 达到阈值或快速轻弹，提交返回
                                            backDispatcher?.onBackPressed()
                                            SwipeBackScrimState.revealEntryId = null
                                            SwipeBackScrimState.progress = 0f
                                            scope.launch {
                                                delay(BACK_COOLDOWN_MS.milliseconds)
                                                backCoolingDown = false
                                            }
                                        } else {
                                            // 慢速拖动未达阈值
                                            scope.launch {
                                                val startNanos = withFrameNanos { it }// 获取屏幕下一帧渲染时间戳
                                                val durationNanos = 160_000_000L // ~160ms 手动回弹
                                                var t = 0f
                                                // 手动回弹
                                                while (t < 1f) {
                                                    val now = withFrameNanos { it }
                                                    t = ((now - startNanos).toFloat() / durationNanos).coerceIn(0f, 1f)
                                                    val p = progress * (1f - t)
                                                    SwipeBackScrimState.progress = p// 当前帧页面停留位置
                                                    backDispatcher?.dispatchOnBackProgressed(
                                                        BackEventCompat(0f, 0f, p, BackEventCompat.EDGE_LEFT)
                                                    )
                                                }
                                                repeat(3) {
                                                    backDispatcher?.dispatchOnBackProgressed(
                                                        BackEventCompat(0f, 0f, 0f, BackEventCompat.EDGE_LEFT)
                                                    )
                                                    withFrameNanos { }
                                                }
                                                backDispatcher?.dispatchOnBackCancelled()
                                                SwipeBackScrimState.revealEntryId = null
                                                SwipeBackScrimState.progress = 0f
                                                backCoolingDown = false
                                            }
                                        }
                                    }
                                } else {
                                    val current = offsetX.value
                                    // 向左快速轻弹
                                    val forwardFling = velocityX <= -flingVelocityPx
                                    scope.launch {
                                        if (forwardActive &&
                                            (current <= -screenWidthPx * forwardThreshold || forwardFling)
                                        ) {
                                            offsetX.animateTo(-screenWidthPx, tween(220))
                                            onCommitForward()
                                            withFrameNanos { }
                                            withFrameNanos { }
                                            offsetX.snapTo(0f)
                                        } else {
                                            offsetX.animateTo(0f, tween(220))
                                        }
                                    }
                                }
                                gestureIsBack = null
                            },
                        )
                    }
                } else Modifier
            ),
    )   {
        val offset = offsetX.value

        // 详情页内容
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }

        // 下一页预览
        if (forwardActive && offset < 0f) {
            val forwardOffsetPx = (offset + screenWidthPx).roundToInt()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(forwardOffsetPx, 0) },
            ) {
                forwardPeek?.invoke()
            }
        }
    }
}

/// 锁定后的处理方式
private enum class LockMode { ACT, CONSUME, RELEASE }

private suspend fun PointerInputScope.detectLockingHorizontalDrag(
    interceptAtDown: (Offset) -> Boolean,
    resolveLock: (isBack: Boolean, position: Offset) -> LockMode,
    onLock: (isBack: Boolean, position: Offset) -> Unit,
    onDrag: (dragAmount: Float, position: Offset) -> Unit,
    onDragEnd: (velocityX: Float) -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val intercept = interceptAtDown(down.position)// 是否拦截
        val pointerId = down.id
        val touchSlop = viewConfiguration.touchSlop// 获取系统默认的滑动阈值
        var accumulatedX = 0f
        var accumulatedY = 0f
        var locked = false
        var acting = false
        val velocityTracker = VelocityTracker()// 追踪松手速度，用于区分快速轻弹与慢速拖动
        velocityTracker.addPosition(down.uptimeMillis, down.position)
        try {
            while (true) {
                val pass = if (locked || intercept) PointerEventPass.Initial else PointerEventPass.Main
                val event = awaitPointerEvent(pass)
                val change = event.changes.firstOrNull { it.id == pointerId } ?: break

                if (change.changedToUpIgnoreConsumed()) {
                    if (locked) change.consume()
                    break
                }// 手指抬起
                velocityTracker.addPosition(change.uptimeMillis, change.position)

                if (locked) {
                    change.consume()
                    if (acting) onDrag(change.positionChangeIgnoreConsumed().x, change.position)
                    continue
                }

                val delta = change.positionChangeIgnoreConsumed()
                if (!intercept && change.isConsumed && (delta.x != 0f || delta.y != 0f)) break
                accumulatedX += delta.x
                accumulatedY += delta.y
                val absX = abs(accumulatedX)
                val absY = abs(accumulatedY)
                when {
                    // 横向滑动
                    absX > touchSlop && absX > absY -> {
                        val isBack = accumulatedX > 0f
                        when (resolveLock(isBack, change.position)) {
                            LockMode.RELEASE -> break
                            LockMode.CONSUME -> {
                                locked = true
                                acting = false
                                change.consume()
                            }
                            LockMode.ACT -> {
                                locked = true
                                acting = true
                                change.consume()
                                onLock(isBack, change.position)
                                onDrag(accumulatedX, change.position)
                            }
                        }
                    }
                    // 纵向滑动让位
                    absY > touchSlop && absY > absX -> break
                }
            }
        } finally {
            if (locked && acting) onDragEnd(velocityTracker.calculateVelocity().x)
        }
    }
}
