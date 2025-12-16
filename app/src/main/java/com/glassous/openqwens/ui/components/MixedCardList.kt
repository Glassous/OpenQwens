package com.glassous.openqwens.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*

/**
 * 混合卡片列表组件 - 将附件卡片和功能卡片合并到同一行显示
 * 采用横向滚动布局，统一的卡片样式，强制完全透明的背景
 */
@Composable
fun MixedCardList(
    selectedAttachments: List<AttachmentData>,
    selectedFunctions: List<SelectedFunction>,
    onRemoveAttachment: (AttachmentData) -> Unit,
    onRemoveFunction: (SelectedFunction) -> Unit,
    showSettings: Boolean = false,
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // 只有当有附件或功能时才显示
    if (selectedAttachments.isNotEmpty() || selectedFunctions.isNotEmpty()) {
        // 使用Box包装，确保背景完全透明
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(Color.Transparent) // 强制透明背景
                .wrapContentHeight()
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent) // 双重透明背景保证
                    .wrapContentHeight(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                // 显示设置卡片（如果在最左侧）
                if (showSettings) {
                    item {
                        Surface(
                            onClick = onSettingsClick,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            shadowElevation = 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "参数设置",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "参数设置",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }

                // 先显示附件卡片
                items(selectedAttachments) { attachment ->
                    AttachmentCard(
                        attachment = attachment,
                        onRemove = { onRemoveAttachment(attachment) }
                    )
                }
                
                // 再显示功能卡片
                items(selectedFunctions) { function ->
                    FunctionCard(
                        selectedFunction = function,
                        onClose = { onRemoveFunction(function) }
                    )
                }
            }
        }
    }
}