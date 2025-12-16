package com.glassous.openqwens.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * AI聊天机器人风格的小功能卡片组件
 * 紧凑的设计，适合横向排列显示
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FunctionCard(
    selectedFunction: SelectedFunction,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(125.dp) // 与附件卡片保持一致的宽度
            .height(45.dp), // 与附件卡片保持一致的高度
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // 关闭按钮 - 右上角
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(20.dp)
                    .padding(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
            
            // 主要内容 - 水平布局，图标和文字在同一行
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 8.dp, end = 24.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                // 功能图标
                Icon(
                    imageVector = selectedFunction.icon,
                    contentDescription = selectedFunction.name,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.width(6.dp))
                
                // 功能名称
                Text(
                    text = selectedFunction.name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * 功能卡片列表组件 - 横向滚动布局
 * 显示多个选中的功能，采用小卡片样式
 */
@Composable
fun FunctionCardList(
    selectedFunctions: List<SelectedFunction>,
    onRemoveFunction: (SelectedFunction) -> Unit,
    modifier: Modifier = Modifier
) {
    if (selectedFunctions.isNotEmpty()) {
        LazyRow(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            items(selectedFunctions) { function ->
                FunctionCard(
                    selectedFunction = function,
                    onClose = { onRemoveFunction(function) }
                )
            }
        }
    }
}