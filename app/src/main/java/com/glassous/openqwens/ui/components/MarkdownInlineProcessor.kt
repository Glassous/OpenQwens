package com.glassous.openqwens.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode

fun processInlineMarkdown(
    node: ASTNode,
    markdown: String,
    builder: AnnotatedString.Builder,
    baseColor: Color,
    baseStyle: TextStyle
) {
    when (node.type) {
        MarkdownElementTypes.STRONG -> {
            val startIndex = builder.length
            node.children.forEach { child ->
                if (child.type != MarkdownTokenTypes.EMPH) {
                    processInlineMarkdown(child, markdown, builder, baseColor, baseStyle)
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
                    processInlineMarkdown(child, markdown, builder, baseColor, baseStyle)
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
                        color = Color.Blue,
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
                builder.append(text)
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
                processInlineMarkdown(child, markdown, builder, baseColor, baseStyle)
            }
        }
    }
}