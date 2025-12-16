package com.glassous.openqwens.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.glassous.openqwens.data.ChatSession
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun ChatSessionItem(
    session: ChatSession,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    // 菜单宽度，需要根据内容调整
    val menuWidth = 120.dp
    val menuWidthPx = with(LocalDensity.current) { menuWidth.toPx() }
    
    // 偏移量，0表示关闭，menuWidthPx表示展开
    val offsetX = remember { Animatable(0f) }
    
    // 当选中状态改变时，重置滑动状态
    LaunchedEffect(isSelected) {
        if (isSelected) {
            offsetX.animateTo(0f)
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min) // 确保高度一致
    ) {
        // 底部操作按钮层 (左侧)
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(menuWidth)
                .fillMaxHeight()
                .padding(start = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 编辑按钮
            IconButton(
                onClick = {
                    scope.launch { offsetX.animateTo(0f) }
                    onRename()
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "编辑",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            // 删除按钮
            IconButton(
                onClick = {
                    scope.launch { offsetX.animateTo(0f) }
                    onDelete()
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
        
        // 顶部内容层 (可滑动)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        // 限制滑动范围：0 到 menuWidthPx (向右滑)
                        // 注意：向右滑 delta > 0，偏移量增加
                        val newValue = (offsetX.value + delta).coerceIn(0f, menuWidthPx)
                        scope.launch { offsetX.snapTo(newValue) }
                    },
                    onDragStopped = {
                        // 释放时根据位置自动吸附
                        val targetValue = if (offsetX.value > menuWidthPx / 2) menuWidthPx else 0f
                        scope.launch {
                            offsetX.animateTo(
                                targetValue = targetValue,
                                animationSpec = tween(durationMillis = 300)
                            )
                        }
                    }
                ),
            color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.small
        ) {
            NavigationDrawerItem(
                label = { 
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = session.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formatDate(session.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                selected = isSelected,
                onClick = {
                    if (offsetX.value > 0) {
                        // 如果已展开，点击则关闭
                        scope.launch { offsetX.animateTo(0f) }
                    } else {
                        onClick()
                    }
                },
                modifier = Modifier.padding(vertical = 2.dp),
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = MaterialTheme.colorScheme.surface // 覆盖默认透明背景，确保遮挡底层
                )
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}