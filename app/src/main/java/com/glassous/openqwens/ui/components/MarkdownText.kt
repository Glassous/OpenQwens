package com.glassous.openqwens.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Check
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.SyntaxThemes
import dev.snipme.highlights.model.SyntaxLanguage
import kotlinx.coroutines.delay

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    linkColor: Color = Color.Blue,
    referenceUrls: Map<Int, String> = emptyMap(),
    onHtmlPreviewClick: ((String) -> Unit)? = null
) {
    val uriHandler = LocalUriHandler.current
    
    // 处理脚注
    val (processedMarkdown, footnotes) = parseFootnoteReferences(markdown)
    
    // 使用GFM（GitHub Flavored Markdown）解析器
    val flavour = GFMFlavourDescriptor()
    val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(processedMarkdown)
    
    val inlineContent = remember { mutableMapOf<String, InlineTextContent>() }

    Column(modifier = modifier) {
        processMarkdownElements(parsedTree, processedMarkdown, color, style, uriHandler, linkColor, referenceUrls, inlineContent, onHtmlPreviewClick)
        
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
    uriHandler: androidx.compose.ui.platform.UriHandler,
    linkColor: Color,
    referenceUrls: Map<Int, String>,
    inlineContent: MutableMap<String, InlineTextContent>,
    onHtmlPreviewClick: ((String) -> Unit)?
) {
    val citationColor = MaterialTheme.colorScheme.primary
    node.children.forEach { child ->
        when (child.type) {
            MarkdownElementTypes.PARAGRAPH -> {
                val paragraphText = child.getTextInNode(markdown).toString()
                
                // 检查是否为表格
                if (isTableContent(paragraphText)) {
                    MarkdownTable(paragraphText, baseColor, baseStyle)
                } else {
                    val annotatedString = buildAnnotatedString {
                        processInlineMarkdown(child, markdown, this, baseColor, baseStyle, linkColor, citationColor, referenceUrls, inlineContent)
                    }
                    MarkdownParagraph(
                        text = annotatedString,
                        style = baseStyle.copy(color = baseColor),
                        inlineContent = inlineContent,
                        onUrlClick = { url -> uriHandler.openUri(url) }
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
                            processMarkdownElements(child, markdown, baseColor, baseStyle, uriHandler, linkColor, referenceUrls, inlineContent, onHtmlPreviewClick)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            MarkdownElementTypes.CODE_FENCE -> {
                val nodeText = child.getTextInNode(markdown).toString()
                val lines = nodeText.lines()
                val firstLine = lines.firstOrNull()?.trim() ?: ""
                
                val language = if (firstLine.startsWith("```")) {
                    firstLine.removePrefix("```").trim()
                } else {
                    ""
                }
                
                val codeText = if (lines.size > 2) {
                     nodeText.substringAfter("\n").substringBeforeLast("```").trim()
                } else {
                     nodeText.removePrefix("```$language").removeSuffix("```").trim()
                }
                
                CodeBlock(codeText, language, baseStyle, baseColor, onHtmlPreviewClick)
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
                                processMarkdownElements(listItem, markdown, baseColor, baseStyle, uriHandler, linkColor, referenceUrls, inlineContent, onHtmlPreviewClick)
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
                                processMarkdownElements(listItem, markdown, baseColor, baseStyle, uriHandler, linkColor, referenceUrls, inlineContent, onHtmlPreviewClick)
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
                    processInlineMarkdown(child, markdown, this, baseColor, baseStyle, linkColor, citationColor, referenceUrls, inlineContent)
                }
                
                MarkdownParagraph(
                    text = annotatedString,
                    style = baseStyle.copy(
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        color = baseColor
                    ),
                    inlineContent = inlineContent,
                    onUrlClick = { url -> uriHandler.openUri(url) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            else -> {
                processMarkdownElements(child, markdown, baseColor, baseStyle, uriHandler, linkColor, referenceUrls, inlineContent, onHtmlPreviewClick)
            }
        }
    }
}

@Composable
fun MarkdownParagraph(
    text: AnnotatedString,
    style: TextStyle,
    inlineContent: Map<String, InlineTextContent>,
    onUrlClick: (String) -> Unit
) {
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
    
    Text(
        text = text,
        modifier = Modifier.pointerInput(onUrlClick) {
            detectTapGestures { pos ->
                layoutResult.value?.let { layout ->
                    val offset = layout.getOffsetForPosition(pos)
                    text.getStringAnnotations("URL", offset, offset)
                        .firstOrNull()?.let { onUrlClick(it.item) }
                }
            }
        },
        style = style,
        inlineContent = inlineContent,
        onTextLayout = { layoutResult.value = it }
    )
}

@Composable
fun CodeBlock(
    codeText: String,
    language: String,
    baseStyle: TextStyle,
    baseColor: Color,
    onHtmlPreviewClick: ((String) -> Unit)? = null
) {
    val clipboardManager = LocalClipboardManager.current
    var isCopied by remember { mutableStateOf(false) }
    val isDarkTheme = isSystemInDarkTheme()

    LaunchedEffect(isCopied) {
        if (isCopied) {
            delay(3000)
            isCopied = false
        }
    }

    // Check if we should show the preview card
    val shouldShowPreview = remember(language) {
        val lang = language.trim().lowercase()
        lang == "html" || lang == "xml" || lang == "svg"
    }

    if (shouldShowPreview && onHtmlPreviewClick != null) {
        HtmlPreviewCard(
            onClick = { onHtmlPreviewClick(codeText) },
            modifier = Modifier.padding(bottom = 8.dp)
        )
    } else {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = language.ifBlank { "Code" },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(codeText))
                            isCopied = true
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Copy code",
                            tint = if (isCopied) Color.Green else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Code
                SelectionContainer {
                    val highlights = remember(codeText, language, isDarkTheme) {
                        try {
                            Highlights.Builder()
                                .code(codeText)
                                .theme(if (isDarkTheme) SyntaxThemes.monokai() else SyntaxThemes.notepad())
                                .language(SyntaxLanguage.getByName(language) ?: SyntaxLanguage.DEFAULT)
                                .build()
                        } catch (e: Exception) {
                            null
                        }
                    }
                    
                    if (highlights != null) {
                        Text(
                            text = highlights.getCode(),
                            modifier = Modifier.padding(12.dp),
                            fontFamily = FontFamily.Monospace,
                            style = baseStyle.copy(
                                fontSize = baseStyle.fontSize * 0.9f
                            )
                        )
                    } else {
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
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}
