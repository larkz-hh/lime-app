package xyz.larkzhh.lime.ui.components

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import xyz.larkzhh.lime.ui.detail.components.EmojiPanel
import xyz.larkzhh.lime.ui.theme.LimeDark
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimeLightGray
import xyz.larkzhh.lime.ui.theme.LimePrimary

/**
 * 评论输入框组件
 *
 * @param hint 输入框提示文字
 * @param isSubmitting 是否正在提交
 * @param selectedImages 已选择的图片列表
 * @param onImagePickRequest 请求选择图片回调
 * @param onRemoveImage 移除指定图片回调
 * @param onSubmit 提交评论回调
 * @param onDismiss 关闭面板回调
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CommentInputSheet(
    hint: String = "说点什么…",
    isSubmitting: Boolean = false,
    selectedImages: List<Uri> = emptyList(),
    onImagePickRequest: () -> Unit = {},
    onRemoveImage: (Uri) -> Unit = {},
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val density = LocalDensity.current
    // 保存文本和光标位置
    val textFieldValueSaver = Saver<TextFieldValue, Pair<String, Int>>(
        save = { it.text to it.selection.end },
        restore = { TextFieldValue(it.first, TextRange(it.second.coerceIn(0, it.first.length))) },
    )
    var textValue by rememberSaveable(stateSaver = textFieldValueSaver) {
        mutableStateOf(TextFieldValue())
    }
    var showEmojiPanel by remember { mutableStateOf(false) }
    var pendingKeyboard by remember { mutableStateOf(false) }// 记录表情到键盘过渡

    val imeHeightPx = WindowInsets.ime.getBottom(density)// 当前软键盘高度
    var savedImeHeight by remember { mutableIntStateOf(0) }// 记录键盘最大高度

    LaunchedEffect(imeHeightPx) {
        val dp = with(density) { imeHeightPx.toDp().value.toInt() }
        if (dp > savedImeHeight) savedImeHeight = dp
        // 过渡状态
        if (pendingKeyboard && savedImeHeight > 0 && dp >= savedImeHeight) {
            showEmojiPanel = false
            pendingKeyboard = false
        }
    }

    /// 输入框主动获取焦点
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    BackHandler {
        if (showEmojiPanel) {
            pendingKeyboard = true
            focusRequester.requestFocus()
            keyboardController?.show()
        } else {
            onDismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { onDismiss() },
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { },
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = MaterialTheme.colorScheme.background,
        ) {
            val colModifier: Modifier = if (!showEmojiPanel && !pendingKeyboard) {
                Modifier.fillMaxWidth().imePadding().navigationBarsPadding()
            } else {
                Modifier.fillMaxWidth().navigationBarsPadding()
            }
            Column(modifier = colModifier) {
                Spacer(modifier = Modifier.height(12.dp))

                // 输入框，独立一行
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                ) {
                    BasicTextField(
                        value = textValue,
                        onValueChange = { newValue -> textValue = newValue },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 40.dp, max = 144.dp)
                            .background(LimeLightGray, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .focusRequester(focusRequester),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = LimeDark,
                            fontSize = 15.sp,
                        ),
                        cursorBrush = SolidColor(LimePrimary),
                        decorationBox = { inner ->
                            if (textValue.text.isEmpty()) {
                                Text(text = hint, color = LimeGray, fontSize = 15.sp)
                            }
                            inner()
                        },
                    )
                    // 拦截点击，切换键盘
                    if (showEmojiPanel && !pendingKeyboard) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                ) {
                                    pendingKeyboard = true
                                    focusRequester.requestFocus()
                                    keyboardController?.show()
                                }
                        )
                    }
                }

                // 已选图片预览行
                if (selectedImages.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(selectedImages, key = { index, _ -> index }) { _, uri ->
                            Box(modifier = Modifier.size(56.dp)) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp)),
                                )
                                // 删除按钮
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(2.dp)
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .clickable { onRemoveImage(uri) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "删除图片",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp),
                                    )
                                }
                            }
                        }
                        // 未满9张时显示添加按钮
                        if (selectedImages.size < 9) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(LimeLightGray)
                                        .clickable { onImagePickRequest() },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = "添加图片",
                                        tint = LimeGray,
                                        modifier = Modifier.size(28.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(top = 8.dp), color = LimeLightGray)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { }) {
                        Icon(Icons.Outlined.Mic, contentDescription = "录音", tint = LimeGray, modifier = Modifier.size(22.dp))
                    }
                    IconButton(onClick = { onImagePickRequest() }) {
                        Icon(Icons.Outlined.Image, contentDescription = "相册", tint = LimeGray, modifier = Modifier.size(22.dp))
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Outlined.CameraAlt, contentDescription = "拍照", tint = LimeGray, modifier = Modifier.size(22.dp))
                    }
                    IconButton(onClick = {
                        if (showEmojiPanel) {
                            pendingKeyboard = true
                            focusRequester.requestFocus()
                            keyboardController?.show()
                        } else {
                            focusManager.clearFocus()
                            showEmojiPanel = true
                        }
                    }) {
                        Icon(
                            imageVector = if (showEmojiPanel) Icons.Outlined.Keyboard else Icons.Outlined.EmojiEmotions,
                            contentDescription = if (showEmojiPanel) "键盘" else "表情",
                            tint = if (showEmojiPanel) LimePrimary else LimeGray,
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // 发送按钮
                    val canSend = textValue.text.isNotBlank() || selectedImages.isNotEmpty()
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = LimePrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (canSend) LimePrimary else LimeLightGray)
                                .clickable(enabled = canSend) {
                                    onSubmit(textValue.text)
                                    textValue = TextFieldValue()
                                }
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "发送",
                                fontSize = 13.sp,
                                color = if (canSend) Color.White else LimeGray,
                            )
                        }
                    }
                }

                val emojiHeight = savedImeHeight.dp.coerceAtLeast(260.dp)

                if (showEmojiPanel) {
                    EmojiPanel(
                        modifier = Modifier.height(emojiHeight),
                        onEmojiClick = { emoji ->
                            val cursor = textValue.selection.end
                            val newText = textValue.text.substring(0, cursor) + emoji + textValue.text.substring(cursor)
                            textValue = TextFieldValue(newText, TextRange(cursor + emoji.length))
                        },
                    )
                } else if (pendingKeyboard) {
                    Spacer(modifier = Modifier.height(emojiHeight))
                }
            }
        }
    }
}
