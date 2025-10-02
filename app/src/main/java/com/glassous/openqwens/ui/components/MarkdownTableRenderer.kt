package com.glassous.openqwens.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MarkdownTable(
    tableContent: String,
    baseColor: Color,
    baseStyle: TextStyle
) {
    val lines = tableContent.trim().split("\n")
    if (lines.size < 2) return
    
    // 解析表头
    val headers = lines[0].split("|").map { it.trim() }.filter { it.isNotEmpty() }
    
    // 跳过分隔符行，解析数据行
    val dataRows = lines.drop(2).map { line ->
        line.split("|").map { it.trim() }.filter { it.isNotEmpty() }
    }.filter { it.isNotEmpty() }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            // 表头
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline)
                    .padding(8.dp)
            ) {
                headers.forEach { header ->
                    Text(
                        text = header,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        style = baseStyle.copy(
                            fontWeight = FontWeight.Bold,
                            color = baseColor
                        )
                    )
                }
            }
            
            // 数据行
            dataRows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline)
                        .padding(8.dp)
                ) {
                    row.take(headers.size).forEach { cell ->
                        Text(
                            text = cell,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp),
                            style = baseStyle.copy(color = baseColor)
                        )
                    }
                    // 如果行的列数少于表头，填充空白
                    repeat(maxOf(0, headers.size - row.size)) {
                        Text(
                            text = "",
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp),
                            style = baseStyle.copy(color = baseColor)
                        )
                    }
                }
            }
        }
    }
}

// 检测是否为表格内容
fun isTableContent(text: String): Boolean {
    val lines = text.trim().split("\n")
    return lines.size >= 2 && 
           lines[0].contains("|") && 
           lines.getOrNull(1)?.matches(Regex("^\\s*[|:\\-\\s]+\\s*$")) == true
}