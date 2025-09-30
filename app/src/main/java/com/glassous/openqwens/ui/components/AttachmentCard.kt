package com.glassous.openqwens.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * 附件卡片组件
 * 宽度比功能卡片宽25%，用于显示上传的附件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentCard(
    attachment: AttachmentData,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(125.dp) // 比功能卡片(100dp)宽25%
            .height(45.dp), // 降低至原来的1/2
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // 删除按钮 - 右上角
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(20.dp)
                    .padding(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "删除附件",
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            
            // 主要内容 - 水平布局
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 8.dp, end = 24.dp, top = 4.dp, bottom = 4.dp), // 右侧留出删除按钮空间
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                // 文件类型图标
                Icon(
                    imageVector = getAttachmentIcon(attachment.attachmentType),
                    contentDescription = attachment.attachmentType.displayName,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.width(6.dp))
                
                // 文件信息列
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    // 文件名
                    Text(
                        text = attachment.getDisplayFileName(12),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // 文件大小
                    Text(
                        text = attachment.getFormattedFileSize(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * 附件卡片列表组件
 * 横向滚动显示多个附件
 */
@Composable
fun AttachmentCardList(
    attachments: List<AttachmentData>,
    onRemoveAttachment: (AttachmentData) -> Unit,
    modifier: Modifier = Modifier
) {
    if (attachments.isNotEmpty()) {
        LazyRow(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(attachments) { attachment ->
                AttachmentCard(
                    attachment = attachment,
                    onRemove = { onRemoveAttachment(attachment) }
                )
            }
        }
    }
}

/**
 * 根据附件类型获取对应的图标
 */
fun getAttachmentIcon(attachmentType: AttachmentType): ImageVector {
    return when (attachmentType) {
        AttachmentType.IMAGE -> Icons.Default.Image
        AttachmentType.VIDEO -> Icons.Default.VideoLibrary
        AttachmentType.AUDIO -> Icons.Default.AudioFile
        AttachmentType.DOCUMENT -> Icons.Default.Description
        AttachmentType.TEXT -> Icons.Default.TextSnippet
        AttachmentType.OTHER -> Icons.Default.InsertDriveFile
    }
}

/**
 * 根据附件类型获取对应的颜色主题
 */
@Composable
fun getAttachmentCardColors(attachmentType: AttachmentType): CardColors {
    return when (attachmentType) {
        AttachmentType.IMAGE -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
        )
        AttachmentType.VIDEO -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
        )
        AttachmentType.AUDIO -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f)
        )
        AttachmentType.DOCUMENT -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
        )
        AttachmentType.TEXT -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        )
        AttachmentType.OTHER -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    }
}