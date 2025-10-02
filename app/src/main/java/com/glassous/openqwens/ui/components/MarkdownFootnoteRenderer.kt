package com.glassous.openqwens.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownFootnotes(
    footnotes: Map<String, String>,
    baseColor: Color,
    baseStyle: TextStyle
) {
    if (footnotes.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        
        Divider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        footnotes.forEach { (id, content) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                Text(
                    text = "[$id]: ",
                    style = baseStyle.copy(
                        fontSize = baseStyle.fontSize * 0.9f,
                        fontWeight = FontWeight.Bold,
                        color = baseColor.copy(alpha = 0.8f)
                    )
                )
                Text(
                    text = content,
                    style = baseStyle.copy(
                        fontSize = baseStyle.fontSize * 0.9f,
                        color = baseColor.copy(alpha = 0.8f)
                    )
                )
            }
        }
    }
}

// 解析脚注引用
fun parseFootnoteReferences(text: String): Pair<String, Map<String, String>> {
    val footnotePattern = Regex("\\[\\^([^\\]]+)\\]")
    val footnoteDefPattern = Regex("\\[\\^([^\\]]+)\\]:\\s*(.+)")
    val refPattern = Regex("ref_(\\d+)")
    
    val footnotes = mutableMapOf<String, String>()
    val lines = text.split("\n").toMutableList()
    
    // 提取脚注定义
    val iterator = lines.iterator()
    while (iterator.hasNext()) {
        val line = iterator.next()
        val match = footnoteDefPattern.find(line)
        if (match != null) {
            val id = match.groupValues[1]
            val content = match.groupValues[2]
            footnotes[id] = content
            iterator.remove()
        }
    }
    
    // 替换脚注引用为上标
    var processedText = lines.joinToString("\n")
    footnotePattern.findAll(processedText).forEach { match ->
        val id = match.groupValues[1]
        if (footnotes.containsKey(id)) {
            processedText = processedText.replace(match.value, "^$id")
        }
    }
    
    // 处理ref_n引用，将其转换为上标格式
    refPattern.findAll(processedText).forEach { match ->
        val refNumber = match.groupValues[1]
        processedText = processedText.replace(match.value, "^[$refNumber]")
    }
    
    return Pair(processedText, footnotes)
}