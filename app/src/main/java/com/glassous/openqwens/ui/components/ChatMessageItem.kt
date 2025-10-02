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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.glassous.openqwens.data.ChatMessage
import com.glassous.openqwens.ui.activities.MessageDetailActivity
import kotlinx.coroutines.launch
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
            // 传递图片数据
            putStringArrayListExtra("image_urls", ArrayList(message.imageUrls))
            putStringArrayListExtra("local_image_paths", ArrayList(message.localImagePaths))
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
            modifier = Modifier.widthIn(max = 350.dp),
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
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    // 显示文本内容
                    if (message.content.isNotBlank()) {
                        // 检查是否为深度思考消息
                        if (message.content.contains("====================思考过程====================") ||
                            message.content.contains("====================完整回复====================")) {
                            // 使用深度思考卡片组件
                            val (reasoningContent, finalContent) = parseDeepThinkingContent(message.content)
                            DeepThinkingCard(
                                reasoningContent = reasoningContent,
                                finalContent = finalContent,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else if (message.content.contains("====================搜索结果====================") ||
                                   message.content.contains("====================联网搜索结果====================") ||
                                   message.content.contains("====================回复内容====================")) {
                            // 使用网络搜索卡片组件
                            val (searchResults, replyContent, mainContent) = parseWebSearchContent(message.content)
                            
                            // 显示搜索结果和回复内容的折叠卡片
                            WebSearchCard(
                                searchResults = searchResults,
                                replyContent = replyContent,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            // 显示正文内容（支持ref_n引用）
                            if (mainContent.isNotBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                MarkdownText(
                                    markdown = mainContent,
                                    color = if (message.isFromUser) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        } else {
                            // 普通消息使用原有的显示方式
                            MarkdownText(
                                markdown = message.content,
                                color = if (message.isFromUser) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    
                    // 显示附件（如果有）
                    if (message.attachments?.isNotEmpty() == true) {
                        if (message.content.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        AttachmentCardList(
                            attachments = message.attachments ?: emptyList(),
                            onRemoveAttachment = { /* 已发送的消息不允许删除附件 */ },
                            showRemoveButton = false, // 不显示删除按钮
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // 显示图片（如果有）
                    if (message.imageUrls.isNotEmpty()) {
                        if (message.content.isNotBlank() || (message.attachments?.isNotEmpty() == true)) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(message.imageUrls) { imageUrl ->
                                // 优先使用本地路径，如果没有则使用网络URL
                                val imageSource = if (message.localImagePaths.isNotEmpty()) {
                                    "file://${message.localImagePaths.firstOrNull() ?: imageUrl}"
                                } else {
                                    imageUrl
                                }
                                
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(imageSource)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "生成的图片",
                                    modifier = Modifier
                                        .size(200.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
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
                // 添加图片数据
                putStringArrayListExtra("image_urls", ArrayList(message.imageUrls))
                putStringArrayListExtra("local_image_paths", ArrayList(message.localImagePaths))
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