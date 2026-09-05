package xyz.larkzhh.lime.ui.group

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import xyz.larkzhh.lime.domain.model.ImGroup
import xyz.larkzhh.lime.navigation.Screen
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimeLightGray
import xyz.larkzhh.lime.ui.theme.LimePrimary

@Composable
fun GroupListScreen(
    navController: NavHostController,
    viewModel: GroupListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var tab by remember { mutableIntStateOf(0) }

    val owned = state.groups.filter { it.ownerId == state.selfImUserId }
    val joined = state.groups.filter { it.ownerId != state.selfImUserId }

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
                    contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Spacer(Modifier.width(2.dp))
            TopBarTab(
                label = "我创建的",
                selected = tab == 0,
                onClick = { tab = 0 },
            )
            TopBarTab(
                label = "我加入的",
                selected = tab == 1,
                onClick = { tab = 1 },
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { navController.navigate(Screen.CreateGroup.route) }) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "创建群聊",
                    tint = LimePrimary,
                )
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = LimeLightGray)

        val list = if (tab == 0) owned else joined
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = LimePrimary)
                }
            }

            list.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (tab == 0) "还没有创建群聊" else "还没有加入群聊",
                        color = LimeGray,
                        fontSize = 14.sp,
                    )
                }
            }

            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(list, key = { it.groupId }) { group ->
                        GroupItem(
                            group = group,
                            onClick = {
                                navController.navigate(Screen.ImChat.createRoute("group_${group.groupId}")) {
                                    launchSingleTop = true
                                }
                            },
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = LimeLightGray)
                    }
                }
            }
        }
    }
}

/// 顶部栏内嵌 tab
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
            color = if (selected) LimePrimary else LimeGray,
        )
        if (selected) {
            Spacer(Modifier.height(3.dp))
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(LimePrimary),
            )
        }
    }
}

@Composable
private fun GroupItem(group: ImGroup, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = group.faceUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(LimeLightGray),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = group.name,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
            group.introduction?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = it,
                    fontSize = 12.sp,
                    color = LimeGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text("${group.memberCount}人", fontSize = 12.sp, color = LimeGray)
    }
}
