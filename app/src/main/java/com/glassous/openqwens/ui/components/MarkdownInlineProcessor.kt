package com.glassous.openqwens.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.em
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode

fun processInlineMarkdown(
    node: ASTNode,
    markdown: String,
    builder: AnnotatedString.Builder,
    baseColor: Color,
    baseStyle: TextStyle,
    linkColor: Color = Color.Blue,
    citationColor: Color = Color.Blue, // 默认为蓝色，实际调用时会传入 Primary Color
    referenceUrls: Map<Int, String> = emptyMap(),
    inlineContent: MutableMap<String, InlineTextContent>? = null
) {
    when (node.type) {
        MarkdownElementTypes.STRONG -> {
            val startIndex = builder.length
            node.children.forEach { child ->
                if (child.type != MarkdownTokenTypes.EMPH) {
                    processInlineMarkdown(child, markdown, builder, baseColor, baseStyle, linkColor, citationColor, referenceUrls, inlineContent)
                }
            }
            val endIndex = builder.length
            builder.addStyle(
                SpanStyle(fontWeight = FontWeight.Bold),
                startIndex,
                endIndex
            )
        }
        
        MarkdownElementTypes.EMPH -> {
            val startIndex = builder.length
            node.children.forEach { child ->
                if (child.type != MarkdownTokenTypes.EMPH) {
                    processInlineMarkdown(child, markdown, builder, baseColor, baseStyle, linkColor, citationColor, referenceUrls, inlineContent)
                }
            }
            val endIndex = builder.length
            builder.addStyle(
                SpanStyle(fontStyle = FontStyle.Italic),
                startIndex,
                endIndex
            )
        }
        
        MarkdownElementTypes.INLINE_LINK -> {
            val linkText = node.children.find { it.type == MarkdownElementTypes.LINK_TEXT }
            val linkDestination = node.children.find { it.type == MarkdownElementTypes.LINK_DESTINATION }
            
            if (linkText != null && linkDestination != null) {
                val startIndex = builder.length
                val text = linkText.getTextInNode(markdown).toString().removeSurrounding("[", "]")
                builder.append(text)
                val endIndex = builder.length
                
                val url = linkDestination.getTextInNode(markdown).toString()
                builder.addStyle(
                    SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline
                    ),
                    startIndex,
                    endIndex
                )
                builder.addStringAnnotation(
                    tag = "URL",
                    annotation = url,
                    start = startIndex,
                    end = endIndex
                )
            }
        }
        
        MarkdownElementTypes.CODE_SPAN -> {
            val startIndex = builder.length
            val codeText = node.getTextInNode(markdown).toString().removeSurrounding("`")
            builder.append(codeText)
            val endIndex = builder.length
            builder.addStyle(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = Color.Gray.copy(alpha = 0.2f),
                    fontSize = baseStyle.fontSize * 0.9f
                ),
                startIndex,
                endIndex
            )
        }
        
        // 任务列表项支持
        MarkdownTokenTypes.TEXT -> {
            val text = node.getTextInNode(markdown).toString()
            // 检查是否是任务列表项
            if (text.startsWith("[ ]") || text.startsWith("[x]") || text.startsWith("[X]")) {
                val isChecked = text.startsWith("[x]") || text.startsWith("[X]")
                val checkbox = if (isChecked) "☑ " else "☐ "
                val taskText = text.substring(3).trim()
                
                val startIndex = builder.length
                builder.append(checkbox)
                builder.append(taskText)
                val endIndex = builder.length
                
                if (isChecked) {
                    builder.addStyle(
                        SpanStyle(textDecoration = TextDecoration.LineThrough),
                        startIndex + 2,
                        endIndex
                    )
                }
            } else {
                // 处理引用标注 SEARCHREFn
                val citationRegex = Regex("SEARCHREF(\\d+)")
                var lastIndex = 0
                
                citationRegex.findAll(text).forEach { matchResult ->
                    // 添加匹配前的文本
                    if (matchResult.range.first > lastIndex) {
                        builder.append(text.substring(lastIndex, matchResult.range.first))
                    }
                    
                    val n = matchResult.groupValues[1]
                    val inlineId = "ref_$n"
                    
                    if (inlineContent != null) {
                        val url = referenceUrls[n.toIntOrNull() ?: 0]
                        val startIndex = builder.length
                        
                        builder.appendInlineContent(inlineId, "[ref_$n]")
                        val endIndex = builder.length
                        
                        if (url != null) {
                            builder.addStringAnnotation(
                                tag = "URL",
                                annotation = url,
                                start = startIndex,
                                end = endIndex
                            )
                        }
                        
                        if (!inlineContent.containsKey(inlineId)) {
                            // 根据数字长度动态计算宽度，避免右侧留白过大
                            val width = if (n.length > 2) 2.4.em else if (n.length > 1) 1.8.em else 1.2.em
                            
                            inlineContent[inlineId] = InlineTextContent(
                                Placeholder(
                                    width = width,
                                    height = 1.4.em,
                                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 1.dp) // 减少外部边距
                                        .background(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 4.dp, vertical = 0.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = n,
                                        style = TextStyle(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            }
                        }
                    } else {
                        // Fallback if inlineContent is not supported (should generally not happen in MarkdownParagraph)
                        builder.append("[ref_$n]")
                    }
                    
                    lastIndex = matchResult.range.last + 1
                }
                
                // 添加剩余文本
                if (lastIndex < text.length) {
                    builder.append(text.substring(lastIndex))
                }
            }
        }
        
        MarkdownTokenTypes.WHITE_SPACE -> {
            builder.append(" ")
        }
        
        MarkdownTokenTypes.EOL -> {
            builder.append("\n")
        }
        
        else -> {
            // 递归处理子节点
            node.children.forEach { child ->
                processInlineMarkdown(child, markdown, builder, baseColor, baseStyle, linkColor, citationColor, referenceUrls, inlineContent)
            }
        }
    }
}