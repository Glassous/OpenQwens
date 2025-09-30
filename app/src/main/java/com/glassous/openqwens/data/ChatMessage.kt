package com.glassous.openqwens.data

import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUrls: List<String> = emptyList(),  // 图片URL列表
    val isImageGeneration: Boolean = false,     // 是否为图片生成消息
    val localImagePaths: List<String> = emptyList()  // 本地图片路径列表
)

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val messages: List<ChatMessage> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)