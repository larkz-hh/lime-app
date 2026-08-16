package xyz.larkzhh.lime.navigation

import androidx.navigation.NavHostController

/// 手势触发的导航动画抑制标志
object SwipeBackNavState {
    var suppressForwardEnter = false
    var suppressPopAnim = false
}

/**
 * 导航到指定用户主页。
 * 当该用户主页已经在返回栈中，就返回过去
 * @param userId 用户id
 * @param selfUserId 当前登录用户 id
 * @param suppressEnterAnimation 是否抑制进场动画
 */
fun NavHostController.navigateToUserProfile(
    userId: Long,
    selfUserId: Long? = null,
    suppressEnterAnimation: Boolean = false,
) {
    SwipeBackNavState.suppressForwardEnter = suppressEnterAnimation

    // 目标是登录用户，优先回到栈里的我的页面
    if (selfUserId != null && userId == selfUserId) {
        if (currentBackStackEntry?.destination?.route == Screen.Profile.route) return
        SwipeBackNavState.suppressPopAnim = true
        if (popBackStack(Screen.Profile.route, inclusive = false)) return
        SwipeBackNavState.suppressPopAnim = false
    }

    val targetRoute = Screen.UserProfile.createRoute(userId)
    // 该用户主页已在栈中，弹回到已有实例
    SwipeBackNavState.suppressPopAnim = true
    if (popBackStack(targetRoute, inclusive = false)) return
    SwipeBackNavState.suppressPopAnim = false

    navigate(targetRoute) { launchSingleTop = true }
}
