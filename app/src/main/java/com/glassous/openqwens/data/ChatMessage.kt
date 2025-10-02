package com.glassous.openqwens.data

import java.util.UUID
import com.glassous.openqwens.ui.components.AttachmentData

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUrls: List<String> = emptyList(),  // 图片URL列表
    val isImageGeneration: Boolean = false,     // 是否为图片生成消息
    val localImagePaths: List<String> = emptyList(),  // 本地图片路径列表
    val attachments: List<AttachmentData>? = emptyList()  // 附件列表，可为null以兼容旧数据
)

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val messages: List<ChatMessage> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)