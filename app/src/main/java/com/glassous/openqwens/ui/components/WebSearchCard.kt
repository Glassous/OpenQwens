    package com.glassous.openqwens.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
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
 * 网络搜索卡片组件
 * 搜索来源可折叠，回复内容直接显示
 */
@Composable
fun WebSearchCard(
    searchResults: String,
    replyContent: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 搜索来源部分 - 可折叠
        if (searchResults.isNotBlank()) {
            ExpandableSection(
                title = "🔍 搜索来源",
                content = searchResults,
                initiallyExpanded = false
            )
            
            if (replyContent.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        
        // 回复内容部分 - 直接显示，无需卡片包装
        if (replyContent.isNotBlank()) {
            MarkdownText(
                markdown = replyContent,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

/**
 * 可展开的内容区域组件
 */
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
            .clickable { isExpanded = !isExpanded }
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
 * 解析网络搜索内容
 * 支持两种格式：
 * 1. ====================搜索结果====================
 * 2. ====================联网搜索结果====================
 */
fun parseWebSearchContent(content: String): Triple<String, String, String> {
    val searchResultMarkers = listOf(
        "====================搜索结果====================",
        "====================联网搜索结果===================="
    )
    val replyContentMarker = "====================回复内容===================="
    
    var searchResults = ""
    var replyContent = ""
    var mainContent = content
    
    // 查找搜索结果标记
    val searchMarker = searchResultMarkers.find { marker ->
        content.contains(marker)
    }
    
    if (searchMarker != null) {
        val searchStartIndex = content.indexOf(searchMarker)
        
        // 查找回复内容标记
        val replyStartIndex = content.indexOf(replyContentMarker)
        
        if (searchStartIndex != -1) {
            if (replyStartIndex != -1 && replyStartIndex > searchStartIndex) {
                // 有搜索结果和回复内容
                searchResults = content.substring(
                    searchStartIndex + searchMarker.length,
                    replyStartIndex
                ).trim()
                
                replyContent = content.substring(
                    replyStartIndex + replyContentMarker.length
                ).trim()
                
                // 主内容是搜索结果标记之前的内容
                mainContent = content.substring(0, searchStartIndex).trim()
            } else {
                // 只有搜索结果，没有回复内容
                searchResults = content.substring(
                    searchStartIndex + searchMarker.length
                ).trim()
                
                // 主内容是搜索结果标记之前的内容
                mainContent = content.substring(0, searchStartIndex).trim()
            }
        }
    } else if (content.contains(replyContentMarker)) {
        // 只有回复内容，没有搜索结果
        val replyStartIndex = content.indexOf(replyContentMarker)
        replyContent = content.substring(
            replyStartIndex + replyContentMarker.length
        ).trim()
        
        // 主内容是回复内容标记之前的内容
        mainContent = content.substring(0, replyStartIndex).trim()
    }
    
    return Triple(searchResults, replyContent, mainContent)
}