package com.glassous.openqwens.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    style: TextStyle = MaterialTheme.typography.bodyMedium
) {
    val uriHandler = LocalUriHandler.current
    
    // 处理脚注
    val (processedMarkdown, footnotes) = parseFootnoteReferences(markdown)
    
    // 使用GFM（GitHub Flavored Markdown）解析器
    val flavour = GFMFlavourDescriptor()
    val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(processedMarkdown)
    
    Column(modifier = modifier) {
        processMarkdownElements(parsedTree, processedMarkdown, color, style, uriHandler)
        
        // 渲染脚注
        MarkdownFootnotes(footnotes, color, style)
    }
}

@Composable
private fun processMarkdownElements(
    node: ASTNode,
    markdown: String,
    baseColor: Color,
    baseStyle: TextStyle,
    uriHandler: androidx.compose.ui.platform.UriHandler
) {
    node.children.forEach { child ->
        when (child.type) {
            MarkdownElementTypes.PARAGRAPH -> {
                val paragraphText = child.getTextInNode(markdown).toString()
                
                // 检查是否为表格
                if (isTableContent(paragraphText)) {
                    MarkdownTable(paragraphText, baseColor, baseStyle)
                } else {
                    val annotatedString = buildAnnotatedString {
                        processInlineMarkdown(child, markdown, this, baseColor, baseStyle)
                    }
                    ClickableText(
                        text = annotatedString,
                        style = baseStyle.copy(color = baseColor),
                        onClick = { offset ->
                            annotatedString.getStringAnnotations(
                                tag = "URL",
                                start = offset,
                                end = offset
                            ).firstOrNull()?.let { annotation ->
                                uriHandler.openUri(annotation.item)
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            MarkdownElementTypes.BLOCK_QUOTE -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(20.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(2.dp)
                                )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            processMarkdownElements(child, markdown, baseColor, baseStyle, uriHandler)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            MarkdownElementTypes.CODE_FENCE -> {
                val codeText = child.getTextInNode(markdown).toString()
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    SelectionContainer {
                        Text(
                            text = codeText,
                            modifier = Modifier.padding(12.dp),
                            fontFamily = FontFamily.Monospace,
                            style = baseStyle.copy(
                                fontSize = baseStyle.fontSize * 0.9f,
                                color = baseColor
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            MarkdownElementTypes.UNORDERED_LIST -> {
                child.children.forEach { listItem ->
                    if (listItem.type == MarkdownElementTypes.LIST_ITEM) {
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "• ",
                                style = baseStyle.copy(color = baseColor),
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Column {
                                processMarkdownElements(listItem, markdown, baseColor, baseStyle, uriHandler)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            MarkdownElementTypes.ORDERED_LIST -> {
                var itemNumber = 1
                child.children.forEach { listItem ->
                    if (listItem.type == MarkdownElementTypes.LIST_ITEM) {
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "$itemNumber. ",
                                style = baseStyle.copy(color = baseColor),
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Column {
                                processMarkdownElements(listItem, markdown, baseColor, baseStyle, uriHandler)
                            }
                        }
                        itemNumber++
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            MarkdownElementTypes.ATX_1, MarkdownElementTypes.ATX_2, MarkdownElementTypes.ATX_3,
            MarkdownElementTypes.ATX_4, MarkdownElementTypes.ATX_5, MarkdownElementTypes.ATX_6 -> {
                val fontSize = when (child.type) {
                    MarkdownElementTypes.ATX_1 -> 24.sp
                    MarkdownElementTypes.ATX_2 -> 20.sp
                    MarkdownElementTypes.ATX_3 -> 18.sp
                    MarkdownElementTypes.ATX_4 -> 16.sp
                    MarkdownElementTypes.ATX_5 -> 14.sp
                    else -> 12.sp
                }
                
                val annotatedString = buildAnnotatedString {
                    processInlineMarkdown(child, markdown, this, baseColor, baseStyle)
                }
                
                Text(
                    text = annotatedString,
                    style = baseStyle.copy(
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        color = baseColor
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            else -> {
                processMarkdownElements(child, markdown, baseColor, baseStyle, uriHandler)
            }
        }
    }
}