package xyz.larkzhh.lime.ui.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.larkzhh.lime.R

private val EMOJI_GROUPS = listOf(
    R.string.emoji_smiley to listOf(
        "😀","😁","😂","🤣","😅","😊","🙂","😉","😍","🥰",
        "😘","😋","😎","🤩","😏","🤔","🤨","😐","😑","😒",
        "😔","😟","😢","😭","😤","😠","😡","🤬","😈","👿",
        "😱","😨","😰","😓","🤗","🥺","😞","😣","😖","🙃",
        "👀","👁️","💀","👻","🤡","😺","😸","😹","😻","🙄",
    ),
    R.string.emoji_gestures to listOf(
        "👍","👎","👌","✌️","🤞","🤟","🤘","🤙","☝️","🙏",
        "✊","👊","💪","👋","🤝","👏","🙌","🤲","🤜","🤛",
        "👈","👉","👆","👇","✋","🤚","🖐️","🖖","💅","🤳",
    ),
    R.string.emoji_hearts to listOf(
        "❤️","🧡","💛","💚","💙","💜","🖤","🤍","🤎","💔",
        "❣️","💕","💞","💓","💗","💖","💘","💝","♥️","💟",
    ),
    R.string.emoji_celebration to listOf(
        "🎉","🎊","🎈","🎁","🏆","🥇","🎀","🎗️","🔥","✨",
        "💫","⭐","🌟","💥","🌈","🌸","🌺","🍀","🎆","🎇",
    ),
    R.string.emoji_animals to listOf(
        "🐶","🐱","🐭","🐹","🐰","🦊","🐻","🐼","🐨","🐯",
        "🦁","🐮","🐷","🐸","🐵","🙈","🙉","🙊","🐔","🐧",
        "🦆","🦅","🦉","🦇","🐺","🐴","🦄","🐝","🦋","🐞",
    ),
    R.string.emoji_food to listOf(
        "🍎","🍊","🍋","🍇","🍓","🫐","🍒","🍑","🥭","🍍",
        "🥥","🍅","🥑","🌽","🥕","🍕","🍔","🌮","🍜","🍱",
        "🍣","🍦","🍩","🎂","🍫","🧋","🍺","🥂","☕","🧃",
    ),
)

@Composable
fun EmojiPanel(
    onEmojiClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedGroup by remember { mutableIntStateOf(0) }

    Column(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
        SecondaryScrollableTabRow(
            selectedTabIndex = selectedGroup,
            edgePadding = 8.dp,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = {},
        ) {
            EMOJI_GROUPS.forEachIndexed { index, (nameRes, _) ->
                Tab(
                    selected = selectedGroup == index,
                    onClick = { selectedGroup = index },
                    text = {
                        Text(
                            text = stringResource(nameRes),
                            fontSize = 13.sp,
                            color = if (selectedGroup == index) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                    },
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(EMOJI_GROUPS[selectedGroup].second) { emoji ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onEmojiClick(emoji) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = emoji, fontSize = 22.sp)
                }
            }
        }
    }
}
