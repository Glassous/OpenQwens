package com.glassous.openqwens.api

import com.glassous.openqwens.data.ChatMessage
import kotlinx.coroutines.delay

class MockChatApi {
    
    private val responses = listOf(
        "你好！我是OpenQwens，很高兴为您服务！",
        "这是一个很有趣的问题，让我想想...",
        "根据我的理解，我认为...",
        "感谢您的提问！这个话题很值得探讨。",
        "我需要更多信息来更好地回答您的问题。",
        "这确实是一个复杂的问题，让我为您详细解释一下。",
        "很抱歉，我可能没有完全理解您的意思，能否再详细说明一下？",
        "基于当前的信息，我的建议是...",
        "这个问题涉及多个方面，让我逐一为您分析。",
        "感谢您的耐心！我正在为您准备最佳答案。"
    )
    
    suspend fun sendMessage(message: String): ChatMessage {
        // 模拟网络延迟
        delay(1000 + (Math.random() * 2000).toLong())
        
        val response = responses.random()
        return ChatMessage(
            content = response,
            isFromUser = false
        )
    }
}