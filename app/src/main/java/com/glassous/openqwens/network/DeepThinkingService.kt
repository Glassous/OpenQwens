package com.glassous.openqwens.network

import com.alibaba.dashscope.aigc.generation.Generation
import com.alibaba.dashscope.aigc.generation.GenerationParam
import com.alibaba.dashscope.aigc.generation.GenerationResult
import com.alibaba.dashscope.common.Message
import com.alibaba.dashscope.common.Role
import com.alibaba.dashscope.exception.ApiException
import com.alibaba.dashscope.exception.InputRequiredException
import com.alibaba.dashscope.exception.NoApiKeyException
import com.alibaba.dashscope.utils.Constants
import com.glassous.openqwens.data.ChatMessage
import com.glassous.openqwens.ui.theme.DashScopeConfigManager
import io.reactivex.Flowable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class DeepThinkingService(
    private val configManager: DashScopeConfigManager
) {
    
    companion object {
        // 阿里云百炼API配置
        private const val BASE_URL = "https://dashscope.aliyuncs.com/api/v1"
        // 深度思考模型
        private const val DEEP_THINKING_MODEL = "qwen-plus-2025-04-28"
        
        init {
            // 设置基础URL
            Constants.baseHttpApiUrl = BASE_URL
        }
    }
    
    /**
     * 深度思考结果数据类
     */
    data class DeepThinkingResult(
        val reasoningContent: String,  // 思考过程
        val finalContent: String       // 最终回复
    )
    
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
     * 构建消息列表（包含历史对话）
     */
    private fun buildMessages(messageHistory: List<ChatMessage>): List<Message> {
        return messageHistory.map { convertToMessage(it) }
    }
    
    /**
     * 深度思考生成（流式输出）
     * @param messageHistory 完整的对话历史记录
     * @param onReasoningContent 思考过程内容回调
     * @param onFinalContent 最终回复内容回调
     * @param onComplete 完成回调
     * @param onError 错误回调
     */
    suspend fun generateDeepThinking(
        messageHistory: List<ChatMessage>,
        onReasoningContent: (String) -> Unit,
        onFinalContent: (String) -> Unit,
        onComplete: (DeepThinkingResult) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val apiKey = configManager.apiKey
            if (apiKey.isBlank()) {
                onError("API Key未配置")
                return@withContext
            }
            
            // 构建包含历史对话的消息列表
            val messages = buildMessages(messageHistory)
            
            // 构建参数
            val param = GenerationParam.builder()
                .apiKey(apiKey)
                .model(configManager.getSelectedModel()?.id ?: DEEP_THINKING_MODEL)
                .enableThinking(true)  // 开启深度思考
                .incrementalOutput(true)  // 开启增量输出
                .resultFormat("message")  // 设置结果格式
                .messages(messages)
                .build()
            
            // 调用API
            val generation = Generation()
            val result: Flowable<GenerationResult> = generation.streamCall(param)
            
            val reasoningContentBuilder = StringBuilder()
            val finalContentBuilder = StringBuilder()
            var isFirstReasoningPrint = true
            var isFirstFinalPrint = true
            
            // 处理流式结果
            result.blockingForEach { generationResult ->
                val reasoning = generationResult.output?.choices?.get(0)?.message?.reasoningContent ?: ""
                val content = generationResult.output?.choices?.get(0)?.message?.content ?: ""
                
                // 处理思考过程
                if (reasoning.isNotEmpty()) {
                    reasoningContentBuilder.append(reasoning)
                    if (isFirstReasoningPrint) {
                        isFirstReasoningPrint = false
                    }
                    onReasoningContent(reasoning)
                }
                
                // 处理最终回复
                if (content.isNotEmpty()) {
                    finalContentBuilder.append(content)
                    if (isFirstFinalPrint) {
                        isFirstFinalPrint = false
                    }
                    onFinalContent(content)
                }
            }
            
            // 完成回调
            val finalResult = DeepThinkingResult(
                reasoningContent = reasoningContentBuilder.toString(),
                finalContent = finalContentBuilder.toString()
            )
            onComplete(finalResult)
            
        } catch (e: ApiException) {
            onError("API调用失败: ${e.message}")
        } catch (e: NoApiKeyException) {
            onError("API Key未配置或无效")
        } catch (e: InputRequiredException) {
            onError("输入参数不完整")
        } catch (e: Exception) {
            onError("深度思考失败: ${e.message}")
        }
    }
    
    /**
     * 为聊天消息生成深度思考回复
     * @param messageHistory 完整的对话历史记录
     * @param onReasoningContent 思考过程内容回调
     * @param onFinalContent 最终回复内容回调
     * @param onComplete 完成回调，返回包含深度思考结果的消息
     * @param onError 错误回调
     */
    suspend fun generateDeepThinkingForChat(
        messageHistory: List<ChatMessage>,
        onReasoningContent: (String) -> Unit,
        onFinalContent: (String) -> Unit,
        onComplete: (ChatMessage) -> Unit,
        onError: (String) -> Unit
    ) {
        generateDeepThinking(
            messageHistory = messageHistory,
            onReasoningContent = onReasoningContent,
            onFinalContent = onFinalContent,
            onComplete = { result ->
                // 创建包含深度思考结果的回复消息
                val assistantMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    content = formatDeepThinkingResponse(result.reasoningContent, result.finalContent),
                    isFromUser = false,
                    timestamp = System.currentTimeMillis()
                )
                onComplete(assistantMessage)
            },
            onError = onError
        )
    }
    
    /**
     * 格式化深度思考响应
     */
    private fun formatDeepThinkingResponse(reasoningContent: String, finalContent: String): String {
        return buildString {
            if (reasoningContent.isNotEmpty()) {
                appendLine("====================思考过程====================")
                appendLine(reasoningContent)
                appendLine()
            }
            if (finalContent.isNotEmpty()) {
                appendLine("====================完整回复====================")
                append(finalContent)
            }
        }
    }
}