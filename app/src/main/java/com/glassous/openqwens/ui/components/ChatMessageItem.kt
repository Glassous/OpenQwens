package com.glassous.openqwens.ui.components

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
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
    onBackdropBlurChanged: (Boolean) -> Unit = {},
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
        onShowSnackbar("已复制到剪贴板")
    }
    
    // 跳转详情页面
    val navigateToDetail = {
        val intent = Intent(context, MessageDetailActivity::class.java).apply {
            putExtra("message_content", message.content)
            putExtra("message_timestamp", message.timestamp)
            putExtra("is_from_user", message.isFromUser)
            putStringArrayListExtra("image_urls", ArrayList(message.imageUrls))
            putStringArrayListExtra("local_image_paths", ArrayList(message.localImagePaths))
        }
        context.startActivity(intent)
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
            if (message.isFromUser) {
                Card(
                    modifier = Modifier.combinedClickable(
                        onClick = { },
                        onLongClick = { showActionMenu = true; onBackdropBlurChanged(true) }
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 4.dp
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
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = { },
                                        onLongClick = { showActionMenu = true; onBackdropBlurChanged(true) }
                                    )
                            ) {
                                AttachmentCardList(
                                    attachments = message.attachments ?: emptyList(),
                                    onRemoveAttachment = { /* 已发送的消息不允许删除附件 */ },
                                    showRemoveButton = false, // 不显示删除按钮
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
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
                                    
                                    Box(
                                        modifier = Modifier
                                            .combinedClickable(
                                                onClick = { },
                                                onLongClick = { showActionMenu = true; onBackdropBlurChanged(true) }
                                            )
                                    ) {
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
                        
                        // 显示视频（如果有）
                        if (!message.videoUrl.isNullOrBlank()) {
                            if (message.content.isNotBlank() || (message.attachments?.isNotEmpty() == true) || message.imageUrls.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            
                            VideoPlayerCard(
                                videoUrl = message.videoUrl!!,
                                onPlay = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW)
                                        intent.setDataAndType(android.net.Uri.parse(message.videoUrl), "video/*")
                                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        onShowSnackbar("无法播放视频")
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .combinedClickable(
                            onClick = { },
                            onLongClick = { showActionMenu = true; onBackdropBlurChanged(true) }
                        )
                ) {
                    // 显示文本内容
                    if (message.content.isNotBlank()) {
                        if (message.content.contains("====================思考过程====================") ||
                            message.content.contains("====================完整回复====================")) {
                            val (reasoningContent, finalContent) = parseDeepThinkingContent(message.content)
                            DeepThinkingCard(
                                reasoningContent = reasoningContent,
                                finalContent = finalContent,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else if (message.content.contains("====================搜索结果====================") ||
                                   message.content.contains("====================联网搜索结果====================") ||
                                   message.content.contains("====================回复内容====================")) {
                            val (searchResults, replyContent, mainContent) = parseWebSearchContent(message.content)
                            WebSearchCard(
                                searchResults = searchResults,
                                replyContent = replyContent,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (mainContent.isNotBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                MarkdownText(
                                    markdown = mainContent,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        } else {
                            MarkdownText(
                                markdown = message.content,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    
                    if (message.attachments?.isNotEmpty() == true) {
                        if (message.content.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { },
                                    onLongClick = { showActionMenu = true; onBackdropBlurChanged(true) }
                                )
                        ) {
                            AttachmentCardList(
                                attachments = message.attachments ?: emptyList(),
                                onRemoveAttachment = { },
                                showRemoveButton = false,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    
                    if (message.imageUrls.isNotEmpty()) {
                        if (message.content.isNotBlank() || (message.attachments?.isNotEmpty() == true)) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(message.imageUrls) { imageUrl ->
                                val imageSource = if (message.localImagePaths.isNotEmpty()) {
                                    "file://${message.localImagePaths.firstOrNull() ?: imageUrl}"
                                } else {
                                    imageUrl
                                }
                                Box(
                                    modifier = Modifier
                                        .combinedClickable(
                                            onClick = { },
                                            onLongClick = { showActionMenu = true; onBackdropBlurChanged(true) }
                                        )
                                ) {
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
                    
                    // 显示视频（如果有）
                    if (!message.videoUrl.isNullOrBlank()) {
                        if (message.content.isNotBlank() || (message.attachments?.isNotEmpty() == true) || message.imageUrls.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        VideoPlayerCard(
                            videoUrl = message.videoUrl!!,
                            onPlay = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW)
                                    intent.setDataAndType(android.net.Uri.parse(message.videoUrl), "video/*")
                                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    onShowSnackbar("无法播放视频")
                                }
                            }
                        )
                    }
                }
            }
            // 删除时间戳显示
        }
    }
    
    // 长按弹窗
    MessageActionMenu(
        isVisible = showActionMenu,
        onDismiss = { showActionMenu = false; onBackdropBlurChanged(false) },
        onCopy = copyToClipboard,
        onViewDetails = { navigateToDetail() }
    )
}

@Composable
fun VideoPlayerCard(
    videoUrl: String,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .size(200.dp, 150.dp)
            .clickable(onClick = onPlay),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // 背景（可以是缩略图，这里暂时用纯色+图标）
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayCircle,
                    contentDescription = "播放视频",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "点击播放视频",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}