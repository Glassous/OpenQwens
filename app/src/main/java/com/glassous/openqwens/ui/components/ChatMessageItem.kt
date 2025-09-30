package com.glassous.openqwens.ui.components

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.glassous.openqwens.data.ChatMessage
import com.glassous.openqwens.ui.activities.MessageDetailActivity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatMessageItem(
    message: ChatMessage,
    onShowSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val context = LocalContext.current
    var showActionMenu by remember { mutableStateOf(false) }
    
    // 复制功能
    val copyToClipboard: () -> Unit = {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("聊天消息", message.content)
        clipboard.setPrimaryClip(clip)
        
        // 显示Snackbar提示
        onShowSnackbar("已复制到剪贴板")
    }
    
    // 跳转详情页面
    val navigateToDetail = {
        val intent = Intent(context, MessageDetailActivity::class.java).apply {
            putExtra("message_content", message.content)
            putExtra("message_timestamp", message.timestamp)
            putExtra("is_from_user", message.isFromUser)
        }
        context.startActivity(intent)
        // 添加转场动画
        (context as? android.app.Activity)?.overridePendingTransition(
            com.glassous.openqwens.R.anim.slide_in_right,
            com.glassous.openqwens.R.anim.slide_out_left
        )
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (message.isFromUser) Alignment.End else Alignment.Start
        ) {
            Card(
                modifier = Modifier.combinedClickable(
                    onClick = { },
                    onLongClick = { showActionMenu = true }
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (message.isFromUser) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (message.isFromUser) 16.dp else 4.dp,
                    bottomEnd = if (message.isFromUser) 4.dp else 16.dp
                )
            ) {
                Text(
                    text = message.content,
                    modifier = Modifier.padding(12.dp),
                    color = if (message.isFromUser) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Text(
                text = timeFormat.format(Date(message.timestamp)),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
    
    // 操作菜单
    MessageActionMenu(
        isVisible = showActionMenu,
        onDismiss = { showActionMenu = false },
        onCopy = copyToClipboard,
        onViewDetails = {
            val intent = Intent(context, MessageDetailActivity::class.java).apply {
                putExtra("message_content", message.content)
                putExtra("message_timestamp", message.timestamp)
                putExtra("is_from_user", message.isFromUser)
            }
            context.startActivity(intent)
            // 添加转场动画
            (context as? Activity)?.overridePendingTransition(
                com.glassous.openqwens.R.anim.slide_in_right,
                com.glassous.openqwens.R.anim.slide_out_left
            )
        }
    )
}