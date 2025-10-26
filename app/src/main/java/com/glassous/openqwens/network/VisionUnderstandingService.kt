package com.glassous.openqwens.network

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult
import com.alibaba.dashscope.common.MultiModalMessage
import com.alibaba.dashscope.common.Role
import com.alibaba.dashscope.exception.ApiException
import com.alibaba.dashscope.exception.InputRequiredException
import com.alibaba.dashscope.exception.NoApiKeyException
import com.alibaba.dashscope.utils.Constants
import com.glassous.openqwens.data.ChatMessage
import com.glassous.openqwens.ui.components.AttachmentData
import com.glassous.openqwens.ui.components.AttachmentType
import com.glassous.openqwens.ui.theme.DashScopeConfigManager
import io.reactivex.Flowable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class VisionUnderstandingService(
    private val configManager: DashScopeConfigManager
) {
    
    companion object {
        // 阿里云百炼API配置
        private const val BASE_URL = "https://dashscope.aliyuncs.com/api/v1"
        // 视觉理解模型
        private const val VISION_MODEL = "qwen-vl-max-latest"
        
        init {
            // 设置基础URL
            Constants.baseHttpApiUrl = BASE_URL
        }
    }

    /**
     * 视觉理解结果数据类
     */
    data class VisionUnderstandingResult(
        val content: String,
        val isComplete: Boolean = false
    )

    /**
     * 将ChatMessage转换为DashScope的MultiModalMessage格式
     */
    private fun convertToMultiModalMessage(
        chatMessage: ChatMessage, 
        attachments: List<AttachmentData> = emptyList()
    ): MultiModalMessage {
        val role = if (chatMessage.isFromUser) Role.USER else Role.ASSISTANT
        val contentList = mutableListOf<Map<String, Any>>()
        
        // 添加文本内容
        if (chatMessage.content.isNotEmpty()) {
            contentList.add(mapOf("text" to chatMessage.content))
        }
        
        // 如果是用户消息且有图片附件，添加图片内容
        if (chatMessage.isFromUser && attachments.isNotEmpty()) {
            attachments.filter { it.attachmentType == AttachmentType.IMAGE }.forEach { attachment ->
                // 构建base64图片URL
                val base64ImageUrl = "data:${attachment.mimeType};base64,${attachment.base64Content}"
                contentList.add(mapOf("image" to base64ImageUrl))
            }
        }
        
        return MultiModalMessage.builder()
            .role(role.getValue())
            .content(contentList)
            .build()
    }

    /**
     * 构建消息列表（包含历史对话和图片）
     */
    private fun buildMultiModalMessages(
        messageHistory: List<ChatMessage>,
        currentAttachments: List<AttachmentData> = emptyList()
    ): List<MultiModalMessage> {
        val messages = mutableListOf<MultiModalMessage>()
        
        // 添加系统消息
        val systemMessage = MultiModalMessage.builder()
            .role(Role.SYSTEM.getValue())
            .content(listOf(mapOf("text" to "你是一个专业的视觉理解助手，能够准确分析和描述图片内容，回答用户关于图片的问题。")))
            .build()
        messages.add(systemMessage)
        
        // 处理历史消息
        messageHistory.forEachIndexed { index, chatMessage ->
            if (chatMessage.isFromUser && index == messageHistory.size - 1) {
                // 最后一条用户消息，包含当前附件
                messages.add(convertToMultiModalMessage(chatMessage, currentAttachments))
            } else {
                // 其他历史消息
                messages.add(convertToMultiModalMessage(chatMessage))
            }
        }
        
        return messages
    }

    /**
     * 视觉理解生成（非流式输出）
     * @param messageHistory 完整的对话历史记录
     * @param attachments 图片附件列表
     * @return 视觉理解结果
     */
    suspend fun generateVisionUnderstanding(
        messageHistory: List<ChatMessage>,
        attachments: List<AttachmentData>
    ): Result<VisionUnderstandingResult> = withContext(Dispatchers.IO) {
        try {
            val apiKey = configManager.apiKey
            if (apiKey.isBlank()) {
                return@withContext Result.failure(Exception("API Key未配置"))
            }
            
            // 过滤出图片附件
            val imageAttachments = attachments.filter { it.attachmentType == AttachmentType.IMAGE }
            if (imageAttachments.isEmpty()) {
                return@withContext Result.failure(Exception("没有找到图片附件"))
            }
            
            // 构建包含历史对话和图片的消息列表
            val messages = buildMultiModalMessages(messageHistory, imageAttachments)
            
            // 构建参数
            val param = MultiModalConversationParam.builder()
                .apiKey(apiKey)
                .model(configManager.getSelectedModel()?.id ?: VISION_MODEL)
                .messages(messages)
                .build()
            
            // 调用API
            val conversation = MultiModalConversation()
            val result: MultiModalConversationResult = conversation.call(param)
            
            // 解析结果
            val content = result.output?.choices?.get(0)?.message?.content?.get(0)?.get("text") as? String ?: ""
            
            Result.success(VisionUnderstandingResult(content = content, isComplete = true))
            
        } catch (e: ApiException) {
            Result.failure(Exception("API调用失败: ${e.message}"))
        } catch (e: NoApiKeyException) {
            Result.failure(Exception("API Key未配置或无效"))
        } catch (e: InputRequiredException) {
            Result.failure(Exception("输入参数不完整"))
        } catch (e: Exception) {
            Result.failure(Exception("视觉理解失败: ${e.message}"))
        }
    }

    /**
     * 视觉理解生成（流式输出）
     * @param messageHistory 完整的对话历史记录
     * @param attachments 图片附件列表
     * @param onContent 内容回调
     * @param onComplete 完成回调
     * @param onError 错误回调
     */
    suspend fun generateVisionUnderstandingStream(
        messageHistory: List<ChatMessage>,
        attachments: List<AttachmentData>,
        onContent: (String) -> Unit,
        onComplete: (VisionUnderstandingResult) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val apiKey = configManager.apiKey
            if (apiKey.isBlank()) {
                onError("API Key未配置")
                return@withContext
            }
            
            // 过滤出图片附件
            val imageAttachments = attachments.filter { it.attachmentType == AttachmentType.IMAGE }
            if (imageAttachments.isEmpty()) {
                onError("没有找到图片附件")
                return@withContext
            }
            
            // 构建包含历史对话和图片的消息列表
            val messages = buildMultiModalMessages(messageHistory, imageAttachments)
            
            // 构建参数
            val param = MultiModalConversationParam.builder()
                .apiKey(apiKey)
                .model(configManager.getSelectedModel()?.id ?: VISION_MODEL)
                .incrementalOutput(true)  // 开启增量输出
                .messages(messages)
                .build()
            
            // 调用API
            val conversation = MultiModalConversation()
            val result: Flowable<MultiModalConversationResult> = conversation.streamCall(param)
            
            val contentBuilder = StringBuilder()
            
            // 处理流式结果
            result.blockingForEach { conversationResult ->
                // 添加安全检查避免索引越界
                val choices = conversationResult.output?.choices
                val content = if (choices != null && choices.isNotEmpty()) {
                    choices[0]?.message?.content?.let { contentList ->
                        if (contentList.isNotEmpty()) {
                            contentList[0]?.get("text") as? String ?: ""
                        } else ""
                    } ?: ""
                } else {
                    ""
                }
                
                if (content.isNotEmpty()) {
                    contentBuilder.append(content)
                    onContent(content)
                }
            }
            
            // 完成回调
            val finalResult = VisionUnderstandingResult(
                content = contentBuilder.toString(),
                isComplete = true
            )
            onComplete(finalResult)
            
        } catch (e: ApiException) {
            onError("API调用失败: ${e.message}")
        } catch (e: NoApiKeyException) {
            onError("API Key未配置或无效")
        } catch (e: InputRequiredException) {
            onError("输入参数不完整")
        } catch (e: Exception) {
            onError("视觉理解失败: ${e.message}")
        }
    }

    /**
     * 为聊天消息生成视觉理解回复
     * @param messageHistory 完整的对话历史记录
     * @param attachments 图片附件列表
     * @param onContent 内容回调（用于流式输出）
     * @param onComplete 完成回调，返回包含视觉理解结果的消息
     * @param onError 错误回调
     */
    suspend fun generateVisionUnderstandingForChat(
        messageHistory: List<ChatMessage>,
        attachments: List<AttachmentData>,
        onContent: (String) -> Unit = {},
        onComplete: (ChatMessage) -> Unit,
        onError: (String) -> Unit
    ) {
        generateVisionUnderstandingStream(
            messageHistory = messageHistory,
            attachments = attachments,
            onContent = onContent,
            onComplete = { result ->
                // 创建包含视觉理解结果的回复消息
                val assistantMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    content = result.content,
                    isFromUser = false,
                    timestamp = System.currentTimeMillis()
                )
                onComplete(assistantMessage)
            },
            onError = onError
        )
    }

    /**
     * 检查是否只有图片附件且没有其他功能
     * @param attachments 附件列表
     * @param selectedFunctions 选中的功能列表
     * @return 是否应该启用视觉理解
     */
    fun shouldEnableVisionUnderstanding(
        attachments: List<AttachmentData>,
        selectedFunctions: List<com.glassous.openqwens.ui.components.SelectedFunction>
    ): Boolean {
        // 必须有图片附件
        val hasImages = attachments.any { it.attachmentType == AttachmentType.IMAGE }
        // 不能有其他功能选择
        val hasOtherFunctions = selectedFunctions.isNotEmpty()
        // 不能有非图片附件
        val hasNonImageAttachments = attachments.any { it.attachmentType != AttachmentType.IMAGE }
        
        return hasImages && !hasOtherFunctions && !hasNonImageAttachments
    }
}