package xyz.larkzhh.lime.ui.follow

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import xyz.larkzhh.lime.feature.follow.R
import xyz.larkzhh.lime.data.network.model.FollowListItem
import xyz.larkzhh.lime.domain.model.FollowActionState
import xyz.larkzhh.lime.domain.model.FollowRelation
import xyz.larkzhh.lime.domain.model.toFollowActionState
import xyz.larkzhh.lime.navigation.route.navigateToUserProfile
import xyz.larkzhh.lime.ui.components.FollowButton
import xyz.larkzhh.lime.ui.components.UnfollowConfirmDialog
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimeTheme
import xyz.larkzhh.lime.ui.theme.LimeWhite
import xyz.larkzhh.lime.core.designsystem.R as DesignSystemR

@Composable
fun FollowListScreen(
    navController: NavHostController,
    viewModel: FollowListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val relations by viewModel.relations.collectAsState()
    val selfUserId = viewModel.currentUserId

    var pendingUnfollow by remember { mutableStateOf<FollowListItem?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        // 顶部栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp)
                .height(54.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(DesignSystemR.string.back),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Spacer(Modifier.width(2.dp))
            TopBarTab(
                label = stringResource(R.string.follow_tab_following),
                selected = uiState.selectedTab == FollowTab.Following,
                onClick = { viewModel.selectTab(FollowTab.Following) },
            )
            TopBarTab(
                label = stringResource(R.string.follow_tab_followers),
                selected = uiState.selectedTab == FollowTab.Followers,
                onClick = { viewModel.selectTab(FollowTab.Followers) },
            )
        }
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))

        val tab = uiState.selectedTab
        val page = if (tab == FollowTab.Following) uiState.following else uiState.followers

        when {
            page.isInitialLoading && page.items.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = LimeWhite,
                        strokeWidth = 2.dp,
                    )
                }
            }

            page.error != null && page.items.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = page.error,
                        color = LimeGray,
                        modifier = Modifier.clickable { viewModel.loadInitial(tab) },
                    )
                }
            }

            page.items.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (tab == FollowTab.Following) stringResource(R.string.follow_empty_following) else stringResource(R.string.follow_empty_followers),
                        color = LimeGray,
                    )
                }
            }

            else -> {
                val listState = rememberLazyListState()
                val shouldLoadMore by remember {
                    derivedStateOf {
                        val info = listState.layoutInfo
                        val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
                        info.totalItemsCount > 0 && last >= info.totalItemsCount - 2
                    }
                }
                LaunchedEffect(shouldLoadMore) {
                    if (shouldLoadMore && page.hasMore) viewModel.loadMore(tab)
                }
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    items(page.items, key = { it.id }) { item ->
                        FollowUserCard(
                            item = item,
                            relation = relations[item.id],
                            fallbackState = if (tab == FollowTab.Following) FollowActionState.Following
                                            else FollowActionState.Follow,
                            onFollowClick = { viewModel.follow(item) },
                            onUnfollowClick = { pendingUnfollow = item },
                            onUserClick = { navController.navigateToUserProfile(item.id) },
                            selfUserId = selfUserId,
                        )
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                            modifier = Modifier.padding(start = 80.dp),
                        )
                    }
                    if (page.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = LimeWhite,
                                    strokeWidth = 2.dp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 取消关注确认弹窗
    pendingUnfollow?.let { item ->
        UnfollowConfirmDialog(
            onCancel = { pendingUnfollow = null },
            onConfirm = {
                viewModel.unfollow(item)
                pendingUnfollow = null
            },
        )
    }
}

/// 顶部栏内的 tab 项（与返回键同行）
@Composable
private fun TopBarTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else LimeGray,
        )
        if (selected) {
            Spacer(Modifier.height(3.dp))
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

/// 关注、粉丝列表卡片
@Composable
private fun FollowUserCard(
    item: FollowListItem,
    relation: FollowRelation?,
    fallbackState: FollowActionState,
    onFollowClick: () -> Unit,
    onUnfollowClick: () -> Unit,
    onUserClick: () -> Unit,
    selfUserId: Long? = null,
) {
    // 自己是目标用户
    val state = if (selfUserId != null && item.id == selfUserId) null
                else relation?.toFollowActionState() ?: fallbackState

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onUserClick,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 头像
            val avatarModifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
            if (item.avatar != null) {
                AsyncImage(
                    model = item.avatar,
                    contentDescription = null,
                    modifier = avatarModifier,
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = avatarModifier.background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = item.nickname.take(1),
                        fontSize = 20.sp,
                        color = LimeGray,
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // 昵称、简介
            Column {
                Text(
                    text = item.nickname,
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.bio?.takeIf { it.isNotBlank() } ?: stringResource(R.string.follow_bio_empty),
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    color = LimeGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // 关注按钮
        if (state != null) {
            FollowButton(
                state = state,
                onClick = if (state == FollowActionState.Follow) onFollowClick else onUnfollowClick,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FollowUserCardPreview() {
    LimeTheme {
        Surface {
            Column {
                FollowUserCard(
                    item = FollowListItem(
                        id = 1,
                        nickname = "taffy",
                        handle = "user_taffy",
                        avatar = null,
                        bio = "这是一个比较长的简介，用来测试单行省略的效果，超出部分会省略",
                    ),
                    relation = FollowRelation(following = true, followedBack = false),
                    fallbackState = FollowActionState.Following,
                    onFollowClick = {},
                    onUnfollowClick = {},
                    onUserClick = {},
                )
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                    modifier = Modifier.padding(start = 80.dp),
                )
                // 互相关注
                FollowUserCard(
                    item = FollowListItem(
                        id = 2,
                        nickname = "111",
                        handle = "user_green",
                        avatar = null,
                        bio = null,
                    ),
                    relation = FollowRelation(following = true, followedBack = true),
                    fallbackState = FollowActionState.Following,
                    onFollowClick = {},
                    onUnfollowClick = {},
                    onUserClick = {},
                )
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                    modifier = Modifier.padding(start = 80.dp),
                )
                // 未关注
                FollowUserCard(
                    item = FollowListItem(
                        id = 3,
                        nickname = "🤣🤣",
                        handle = "user_purple",
                        avatar = null,
                        bio = "hhhhhh",
                    ),
                    relation = FollowRelation(following = false, followedBack = true),
                    fallbackState = FollowActionState.Follow,
                    onFollowClick = {},
                    onUnfollowClick = {},
                    onUserClick = {},
                )
            }
        }
    }
}
