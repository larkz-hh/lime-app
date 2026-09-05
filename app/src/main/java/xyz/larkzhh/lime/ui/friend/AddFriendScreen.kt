package xyz.larkzhh.lime.ui.friend

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil3.BitmapImage
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.navigation.Screen
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimeLightGray
import xyz.larkzhh.lime.ui.theme.LimePrimary
import xyz.larkzhh.lime.ui.theme.LimeWhite
import xyz.larkzhh.lime.util.copyToClipboard
import xyz.larkzhh.lime.util.generateGradientQrBitmap
import xyz.larkzhh.lime.util.limeUserQrContent
import xyz.larkzhh.lime.util.saveBitmapToGallery
import xyz.larkzhh.lime.util.showToast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFriendScreen(
    navController: NavHostController,
    viewModel: AddFriendViewModel = hiltViewModel(),
) {
    val user by viewModel.user.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val current = user
    val limeId = current?.handle
    val qrUid = current?.uid ?: current?.handle
    val qrContent = remember(qrUid) { qrUid?.let { limeUserQrContent(it) } }
    val densityValue = LocalDensity.current.density
    val qrSizePx = remember { (240 * densityValue).roundToInt() }
    // 头像
    val logoBitmap by produceState<Bitmap?>(null, current?.avatar) {
        val url = current?.avatar ?: return@produceState
        value = runCatching {
            val result = SingletonImageLoader.get(context).execute(
                ImageRequest.Builder(context)
                    .data(url)
                    .allowHardware(false)
                    .build()
            )
            ((result as? SuccessResult)?.image as? BitmapImage)?.bitmap
        }.getOrNull()
    }
    val qrBitmap = remember(qrContent, qrSizePx, logoBitmap) {
        qrContent?.let {
            generateGradientQrBitmap(it, qrSizePx, logo = logoBitmap)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "添加好友",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        if (current == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("正在加载…", color = LimeGray)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            // 我的二维码
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = LimeWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .padding(1.dp)
                    .background(LimeLightGray, RoundedCornerShape(17.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .background(Color.White, RoundedCornerShape(10.dp)),
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "我的二维码",
                            modifier = Modifier.size(240.dp),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .background(LimeLightGray),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("二维码生成失败", color = LimeGray, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = current.nickname,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Lime ID
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Lime ID：$limeId",
                    fontSize = 15.sp,
                    color = LimeGray,
                )
                Spacer(modifier = Modifier.width(2.dp))
                IconButton(
                    onClick = {
                        limeId?.let {
                            it.copyToClipboard(context)
                            "已复制 Lime ID".showToast(context)
                        }
                    },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "复制 Lime ID",
                        tint = LimeGray,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "保存二维码，让好友扫一扫添加你",
                fontSize = 12.sp,
                color = LimeGray,
            )

            Spacer(modifier = Modifier.height(24.dp))
            // 操作列表
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = LimeWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ActionRow(
                    icon = {
                        Icon(
                            Icons.Filled.QrCodeScanner,
                            contentDescription = null,
                            tint = LimePrimary,
                            modifier = Modifier.size(22.dp),
                        )
                    },
                    label = "扫一扫",
                    onClick = { navController.navigate(Screen.QrScan.route) },
                )
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = LimeLightGray,
                    modifier = Modifier.padding(start = 54.dp),
                )
                ActionRow(
                    icon = {
                        Icon(
                            Icons.Outlined.Download,
                            contentDescription = null,
                            tint = LimePrimary,
                            modifier = Modifier.size(22.dp),
                        )
                    },
                    label = "保存二维码",
                    onClick = {
                        if (qrBitmap == null) {
                            "二维码尚未生成".showToast(context)
                        } else {
                            scope.launch {
                                val ok = saveBitmapToGallery(context,
                                    qrBitmap, "lime_qr_$limeId.jpg")
                                if (ok) "已保存到相册".showToast(context) else "保存失败".showToast(context)
                            }
                        }
                    },
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ActionRow(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = Color(0xFFD0D0D0),
            modifier = Modifier.size(20.dp),
        )
    }
}
