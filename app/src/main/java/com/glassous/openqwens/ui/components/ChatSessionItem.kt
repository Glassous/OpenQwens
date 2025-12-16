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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.changedToUp
import kotlin.math.absoluteValue
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
    onSwipeToClose: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    // 菜单宽度，需要根据内容调整
    val menuWidth = 120.dp
    val menuWidthPx = with(LocalDensity.current) { menuWidth.toPx() }
    
    val viewConfiguration = LocalViewConfiguration.current
    
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
                .padding(start = 16.dp)
                // 根据滑动距离控制透明度，实现未滑动时不可见
                .alpha((offsetX.value / menuWidthPx).coerceIn(0f, 1f)),
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
                .pointerInput(Unit) {
                    val velocityTracker = VelocityTracker()
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var drag: androidx.compose.ui.input.pointer.PointerInputChange? = null
                        var totalDx = 0f
                        
                        // 检测滑动开始
                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (change.changedToUp()) break
                            
                            val dx = change.position.x - change.previousPosition.x
                            totalDx += dx
                            
                            if (totalDx.absoluteValue > viewConfiguration.touchSlop) {
                                // 判断滑动方向
                                val isRightSwipe = totalDx > 0
                                // 只有当处于关闭状态且向右滑动，或者已经打开时，才拦截事件
                                // 如果处于关闭状态且向左滑动，则不拦截，让父容器（侧边栏）处理关闭手势
                                val shouldIntercept = offsetX.value > 0.5f || isRightSwipe
                                
                                if (!shouldIntercept) {
                                    // 拒绝拦截，直接返回，让父容器处理
                                    return@awaitEachGesture
                                }
                                
                                // 确认拦截，开始处理滑动
                                drag = change
                                change.consume()
                                break
                            }
                        } while (true)
                        
                        // 处理后续滑动
                        if (drag != null) {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                
                                if (change == null || change.changedToUp()) {
                                    // 滑动结束，处理惯性
                                    val velocity = velocityTracker.calculateVelocity().x
                                    // 展开条件：速度向右足够大，或者偏移量超过一半且没有快速向左滑动
                                    val targetValue = if (velocity > 1000f || (offsetX.value > menuWidthPx / 2 && velocity > -1000f)) {
                                        menuWidthPx
                                    } else {
                                        0f
                                    }
                                    
                                    scope.launch {
                                        offsetX.animateTo(
                                            targetValue = targetValue,
                                            animationSpec = tween(durationMillis = 300)
                                        )
                                    }
                                    break
                                }
                                
                                val dx = change.position.x - change.previousPosition.x
                                change.consume()
                                velocityTracker.addPosition(change.uptimeMillis, change.position)
                                
                                // 更新偏移量
                                val newValue = (offsetX.value + dx).coerceIn(0f, menuWidthPx)
                                scope.launch { offsetX.snapTo(newValue) }
                            }
                        }
                    }
                },
            color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent,
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
                    unselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}