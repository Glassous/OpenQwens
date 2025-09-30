package com.glassous.openqwens.api

import com.alibaba.dashscope.aigc.generation.Generation
import com.alibaba.dashscope.aigc.generation.GenerationParam
import com.alibaba.dashscope.aigc.generation.GenerationResult
import com.alibaba.dashscope.common.Message
import com.alibaba.dashscope.common.Role
import com.alibaba.dashscope.exception.ApiException
import com.alibaba.dashscope.exception.InputRequiredException
import com.alibaba.dashscope.exception.NoApiKeyException
import com.glassous.openqwens.data.ChatMessage
import com.glassous.openqwens.ui.theme.DashScopeConfigManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.ArrayList

class DashScopeApi(private val configManager: DashScopeConfigManager) {
    
    private val generation = Generation()
    
    /**
     * 将ChatMessage转换为DashScope的Message格式
     */
    private fun convertToMessage(chatMessage: ChatMessage): Message {
        val role = if (chatMessage.isFromUser) Role.USER else Role.ASSISTANT
        return Message.builder()
            .role(role.getValue())
            .content(chatMessage.content)
            .build()
    }
    
    /**
     * 构建包含系统消息和历史对话的消息列表
     */
    private fun buildMessages(messageHistory: List<ChatMessage>): List<Message> {
        val messages = ArrayList<Message>()
        
        // 添加系统消息
        val systemMsg = Message.builder()
            .role(Role.SYSTEM.getValue())
            .content("You are a helpful assistant.")
            .build()
        messages.add(systemMsg)
        
        // 添加历史对话消息
        for (chatMessage in messageHistory) {
            messages.add(convertToMessage(chatMessage))
        }
        
        return messages
    }
    
    /**
     * 发送消息（支持多轮对话）
     * @param messageHistory 完整的对话历史记录
     * @return AI回复的消息
     */
    suspend fun sendMessage(messageHistory: List<ChatMessage>): ChatMessage = withContext(Dispatchers.IO) {
        try {
            val apiKey = configManager.apiKey
            if (apiKey.isEmpty()) {
                throw IllegalStateException("API密钥未配置，请在设置中配置API密钥")
            }
            
            val selectedModel = configManager.getSelectedModel()
            if (selectedModel == null) {
                throw IllegalStateException("未选择模型，请在设置中选择一个模型")
            }
            
            // 构建包含历史对话的消息列表
            val messages = buildMessages(messageHistory)
            
            // 构建请求参数
            val param = GenerationParam.builder()
                .apiKey(apiKey)
                .model(selectedModel.id)
                .messages(messages)
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .build()
            
            // 调用API
            val result: GenerationResult = generation.call(param)
            
            // 解析响应
            val choices = result.output?.choices
            if (choices != null && choices.isNotEmpty()) {
                val responseContent = choices[0].message?.content ?: "抱歉，没有收到有效的回复"
                ChatMessage(
                    content = responseContent,
                    isFromUser = false
                )
            } else {
                ChatMessage(
                    content = "抱歉，API返回了空的响应",
                    isFromUser = false
                )
            }
            
        } catch (e: NoApiKeyException) {
            ChatMessage(
                content = "API密钥错误或未配置，请检查设置中的API密钥配置",
                isFromUser = false
            )
        } catch (e: ApiException) {
            ChatMessage(
                content = "API调用失败：${e.message}",
                isFromUser = false
            )
        } catch (e: InputRequiredException) {
            ChatMessage(
                content = "请求参数不完整：${e.message}",
                isFromUser = false
            )
        } catch (e: IllegalStateException) {
            ChatMessage(
                content = e.message ?: "配置错误",
                isFromUser = false
            )
        } catch (e: Exception) {
            ChatMessage(
                content = "发生未知错误：${e.message}",
                isFromUser = false
            )
        }
    }
}