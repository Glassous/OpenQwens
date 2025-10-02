package com.glassous.openqwens.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 深度思考卡片组件
 * 支持展开折叠功能，使用Material Design 3规范
 */
@Composable
fun DeepThinkingCard(
    reasoningContent: String,
    finalContent: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 思考过程部分 - 可折叠
        if (reasoningContent.isNotBlank()) {
            ExpandableSection(
                title = "💭 思考过程",
                content = reasoningContent,
                initiallyExpanded = false
            )
            
            if (finalContent.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        
        // 完整回复部分 - 直接显示，无需卡片包装
        if (finalContent.isNotBlank()) {
            MarkdownText(
                markdown = finalContent,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

/**
 * 可展开的内容区域组件
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExpandableSection(
    title: String,
    content: String,
    initiallyExpanded: Boolean = false
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "arrow_rotation"
    )

    // 使用透明背景，让卡片与气泡融合
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { isExpanded = !isExpanded },
                onLongClick = { /* 不处理长按，让事件传播到父组件 */ }
            )
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(8.dp) // 使用较小的圆角
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "收起" else "展开",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(rotationAngle)
                )
            }
            
            // 内容区域
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(animationSpec = tween(300)) + expandVertically(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(animationSpec = tween(300))
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    MarkdownText(
                        markdown = content,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

/**
 * 解析深度思考消息内容
 * 从格式化的消息中提取思考过程和完整回复
 */
fun parseDeepThinkingContent(content: String): Pair<String, String> {
    val reasoningMarker = "====================思考过程===================="
    val finalMarker = "====================完整回复===================="
    
    var reasoningContent = ""
    var finalContent = ""
    
    val lines = content.lines()
    var currentSection = ""
    val reasoningLines = mutableListOf<String>()
    val finalLines = mutableListOf<String>()
    
    for (line in lines) {
        when {
            line.contains(reasoningMarker) -> {
                currentSection = "reasoning"
            }
            line.contains(finalMarker) -> {
                currentSection = "final"
            }
            currentSection == "reasoning" && line.trim().isNotEmpty() -> {
                reasoningLines.add(line)
            }
            currentSection == "final" && line.trim().isNotEmpty() -> {
                finalLines.add(line)
            }
        }
    }
    
    reasoningContent = reasoningLines.joinToString("\n").trim()
    finalContent = finalLines.joinToString("\n").trim()
    
    return Pair(reasoningContent, finalContent)
}