package com.glassous.openqwens.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.glassous.openqwens.api.DashScopeApi
import com.glassous.openqwens.data.ChatMessage
import com.glassous.openqwens.data.ChatRepository
import com.glassous.openqwens.data.ChatSession
import com.glassous.openqwens.network.ImageGenerationService
import com.glassous.openqwens.network.VideoGenerationService
import com.glassous.openqwens.network.DeepThinkingService
import com.glassous.openqwens.network.WebSearchService
import com.glassous.openqwens.network.VisionUnderstandingService
import com.glassous.openqwens.ui.components.SelectedFunction
import com.glassous.openqwens.ui.components.AttachmentData
import com.glassous.openqwens.ui.theme.GlobalDashScopeConfigManager
import com.glassous.openqwens.utils.ImageDownloadManager
import com.glassous.openqwens.utils.MediaDownloadManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    
    private val configManager = GlobalDashScopeConfigManager.getInstance(application)
    private val chatApi = DashScopeApi(configManager)
    private val imageDownloadManager = ImageDownloadManager(application)
    private val mediaDownloadManager = MediaDownloadManager(application)
    private val imageGenerationService = ImageGenerationService(configManager, imageDownloadManager)
    private val videoGenerationService = VideoGenerationService(configManager, mediaDownloadManager)
    private val deepThinkingService = DeepThinkingService(configManager)
    private val webSearchService = WebSearchService(configManager)
    private val visionUnderstandingService = VisionUnderstandingService(configManager)
    private val repository = ChatRepository(application)
    
    private val _currentSession = MutableStateFlow<ChatSession?>(null)
    val currentSession: StateFlow<ChatSession?> = _currentSession.asStateFlow()
    
    private val _chatSessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val chatSessions: StateFlow<List<ChatSession>> = _chatSessions.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // 流式输出相关状态
    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()
    
    private val _streamingContent = MutableStateFlow("")
    val streamingContent: StateFlow<String> = _streamingContent.asStateFlow()
    
    // 生成参数状态
    private val _imageGenerationParams = MutableStateFlow(com.glassous.openqwens.data.ImageGenerationParams())
    val imageGenerationParams: StateFlow<com.glassous.openqwens.data.ImageGenerationParams> = _imageGenerationParams.asStateFlow()
    
    private val _videoGenerationParams = MutableStateFlow(com.glassous.openqwens.data.VideoGenerationParams())
    val videoGenerationParams: StateFlow<com.glassous.openqwens.data.VideoGenerationParams> = _videoGenerationParams.asStateFlow()

    init {
        // 加载保存的聊天记录
        loadSavedSessions()
        // 应用启动时总是创建新对话
        createNewChat()
    }
    
    fun loadSavedSessions() {
        val savedSessions = repository.loadSessions()
        _chatSessions.value = savedSessions
    }
    
    private fun saveSessions() {
        repository.saveSessions(_chatSessions.value)
    }
    
    fun resetToNewChat() {
        // 重新进入app时调用，确保以新对话开始
        createNewChat()
    }
    
    fun createNewChat() {
        // 清空主页，不立即创建新对话
        _currentSession.value = null
    }

    fun updateImageGenerationParams(params: com.glassous.openqwens.data.ImageGenerationParams) {
        _imageGenerationParams.value = params
    }

    fun updateVideoGenerationParams(params: com.glassous.openqwens.data.VideoGenerationParams) {
        _videoGenerationParams.value = params
    }
    
    fun sendMessage(content: String, selectedFunctions: List<SelectedFunction> = emptyList(), selectedAttachments: List<AttachmentData> = emptyList()) {
        // 如果当前没有会话，创建一个临时会话用于显示对话
        val currentSession = _currentSession.value ?: run {
            val tempSession = ChatSession(title = "新对话")
            _currentSession.value = tempSession
            tempSession
        }
        
        // 立即设置加载状态为true，显示加载动画
        _isLoading.value = true
        
        // 添加用户消息
        val userMessage = ChatMessage(content = content, isFromUser = true, attachments = selectedAttachments)
        val updatedMessages = currentSession.messages + userMessage
        val updatedSession = currentSession.copy(messages = updatedMessages)
        
        _currentSession.value = updatedSession
        
        // 检查是否选择了图片生成功能、深度思考功能、联网搜索功能或视觉理解功能
        val hasImageGeneration = selectedFunctions.any { it.id == "image_generation" }
        val hasVideoGeneration = selectedFunctions.any { it.id == "video_generation" }
        val hasDeepThinking = selectedFunctions.any { it.id == "deep_thinking" }
        val hasWebSearch = selectedFunctions.any { it.id == "web_search" }
        val hasVisionUnderstanding = selectedFunctions.any { it.id == "vision_understanding" } || 
                                   visionUnderstandingService.shouldEnableVisionUnderstanding(selectedAttachments, selectedFunctions)
        
        // 发送到API并获取回复（传递完整的消息历史）
        viewModelScope.launch {
            try {
                val response = if (hasImageGeneration) {
                    // 使用图片生成服务
                    val imageResult = imageGenerationService.generateImageForChat(
                        userMessage, 
                        content,
                        _imageGenerationParams.value
                    )
                    if (imageResult.isSuccess) {
                        imageResult.getOrThrow()
                    } else {
                        ChatMessage(
                            content = "抱歉，图片生成失败，请稍后重试。",
                            isFromUser = false
                        )
                    }
                } else if (hasVideoGeneration) {
                    // 使用视频生成服务
                    // 检查是否有图片附件用于图生视频
                    val inputImage = selectedAttachments.find { it.mimeType.startsWith("image/") }
                    val videoResult = videoGenerationService.generateVideoForChat(
                        userMessage, 
                        content,
                        inputImage?.uri?.toString(),
                        _videoGenerationParams.value
                    )
                    
                    if (videoResult.isSuccess) {
                        videoResult.getOrThrow()
                    } else {
                        ChatMessage(
                            content = "抱歉，视频生成失败，请稍后重试。",
                            isFromUser = false
                        )
                    }
                } else if (hasDeepThinking) {
                    // 使用深度思考服务
                    var deepThinkingResponse: ChatMessage? = null
                    
                    deepThinkingService.generateDeepThinkingForChat(
                        messageHistory = updatedMessages,
                        onReasoningContent = { reasoning ->
                            // 可以在这里处理思考过程的实时显示，暂时不处理
                        },
                        onFinalContent = { finalContent ->
                            // 可以在这里处理最终回复的实时显示，暂时不处理
                        },
                        onComplete = { chatMessage ->
                            deepThinkingResponse = chatMessage
                        },
                        onError = { errorMessage ->
                            deepThinkingResponse = ChatMessage(
                                content = "抱歉，深度思考功能暂时不可用：$errorMessage",
                                isFromUser = false
                            )
                        }
                    )
                    
                    // 返回深度思考的结果
                    deepThinkingResponse ?: ChatMessage(
                        content = "抱歉，深度思考功能暂时不可用，请稍后重试。",
                        isFromUser = false
                    )
                } else if (hasWebSearch) {
                    // 使用联网搜索服务
                    val searchResult = webSearchService.performWebSearchForChat(updatedMessages)
                    if (searchResult.isSuccess) {
                        searchResult.getOrThrow()
                    } else {
                        ChatMessage(
                            content = "抱歉，联网搜索失败，请稍后重试。",
                            isFromUser = false
                        )
                    }
                } else if (hasVisionUnderstanding) {
                    // 使用视觉理解服务 - 非流式模式，直接调用流式方法并等待完成
                    var visionResponse: ChatMessage? = null
                    var isCompleted = false
                    
                    visionUnderstandingService.generateVisionUnderstandingStream(
                        messageHistory = updatedMessages,
                        attachments = selectedAttachments,
                        onContent = { /* 忽略流式内容 */ },
                        onComplete = { result ->
                            visionResponse = ChatMessage(content = result.content, isFromUser = false)
                            isCompleted = true
                        },
                        onError = { error ->
                            visionResponse = ChatMessage(
                                content = "抱歉，视觉理解失败：$error",
                                isFromUser = false
                            )
                            isCompleted = true
                        }
                    )
                    
                    // 等待完成
                    while (!isCompleted) {
                        kotlinx.coroutines.delay(100)
                    }
                    
                    visionResponse ?: ChatMessage(
                        content = "抱歉，视觉理解失败，请稍后重试。",
                        isFromUser = false
                    )
                } else {
                    // 使用普通聊天API
                    chatApi.sendMessage(updatedMessages)
                }
                
                // 只有在API响应成功后才添加AI回复消息
                val finalMessages = updatedMessages + response
                val finalSession = updatedSession.copy(messages = finalMessages)
                
                _currentSession.value = finalSession
                
                // 第一段对话完成后，将会话添加到会话列表并保存
                if (!_chatSessions.value.any { it.id == finalSession.id }) {
                    _chatSessions.value = listOf(finalSession) + _chatSessions.value
                }
                updateSessionInList(finalSession)
                
                // 保存到本地存储
                saveSessions()
            } catch (e: Exception) {
                // 处理错误 - 只有在出错时才添加错误消息
                val errorMessage = ChatMessage(
                    content = "抱歉，发生了错误，请稍后重试。",
                    isFromUser = false
                )
                val errorMessages = updatedMessages + errorMessage
                val errorSession = updatedSession.copy(messages = errorMessages)
                
                _currentSession.value = errorSession
                
                // 即使出错，也要将会话添加到列表中
                if (!_chatSessions.value.any { it.id == errorSession.id }) {
                    _chatSessions.value = listOf(errorSession) + _chatSessions.value
                }
                updateSessionInList(errorSession)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 流式发送消息（支持实时输出）
     */
    fun sendMessageStream(content: String, selectedFunctions: List<SelectedFunction> = emptyList(), selectedAttachments: List<AttachmentData> = emptyList()) {
        // 如果当前没有会话，创建一个临时会话用于显示对话
        val currentSession = _currentSession.value ?: run {
            val tempSession = ChatSession(title = "新对话")
            _currentSession.value = tempSession
            tempSession
        }
        
        // 检查是否选择了图片生成功能、深度思考功能、联网搜索功能或视觉理解功能
        val hasImageGeneration = selectedFunctions.any { it.id == "image_generation" }
        val hasVideoGeneration = selectedFunctions.any { it.id == "video_generation" }
        val hasDeepThinking = selectedFunctions.any { it.id == "deep_thinking" }
        val hasWebSearch = selectedFunctions.any { it.id == "web_search" }
        val hasVisionUnderstanding = selectedFunctions.any { it.id == "vision_understanding" } || 
                                   visionUnderstandingService.shouldEnableVisionUnderstanding(selectedAttachments, selectedFunctions)
        
        if (hasImageGeneration || hasVideoGeneration || hasDeepThinking) {
            // 图片/视频生成和深度思考不支持流式输出，使用普通发送方法
            sendMessage(content, selectedFunctions, selectedAttachments)
            return
        }
        
        // 设置流式输出状态
        _isStreaming.value = true
        _streamingContent.value = ""
        
        // 添加用户消息
        val userMessage = ChatMessage(content = content, isFromUser = true, attachments = selectedAttachments)
        val updatedMessages = currentSession.messages + userMessage
        val updatedSession = currentSession.copy(messages = updatedMessages)
        
        _currentSession.value = updatedSession
        
        // 创建一个临时的AI消息用于显示流式内容
        val tempAiMessage = ChatMessage(content = "", isFromUser = false)
        val messagesWithTemp = updatedMessages + tempAiMessage
        val sessionWithTemp = updatedSession.copy(messages = messagesWithTemp)
        _currentSession.value = sessionWithTemp
        
        // 发送流式请求
        viewModelScope.launch {
            if (hasWebSearch) {
                // 使用联网搜索的流式输出
                webSearchService.performWebSearchForChatStream(
                    messageHistory = updatedMessages,
                    onContent = { contentChunk ->
                        // 更新流式内容
                        _streamingContent.value += contentChunk
                        
                        // 更新临时消息的内容
                        val currentMessages = _currentSession.value?.messages?.toMutableList() ?: return@performWebSearchForChatStream
                        if (currentMessages.isNotEmpty()) {
                            val lastIndex = currentMessages.size - 1
                            currentMessages[lastIndex] = currentMessages[lastIndex].copy(content = _streamingContent.value)
                            val updatedSessionWithStream = _currentSession.value?.copy(messages = currentMessages)
                            _currentSession.value = updatedSessionWithStream
                        }
                    },
                    onComplete = { finalMessage ->
                        // 流式输出完成，使用最终的AI消息
                        val finalMessages = updatedMessages + finalMessage
                        val finalSession = updatedSession.copy(messages = finalMessages)
                        
                        _currentSession.value = finalSession
                        
                        // 第一段对话完成后，将会话添加到会话列表并保存
                        if (!_chatSessions.value.any { it.id == finalSession.id }) {
                            _chatSessions.value = listOf(finalSession) + _chatSessions.value
                        }
                        updateSessionInList(finalSession)
                        
                        // 保存到本地存储
                        saveSessions()
                        
                        // 重置流式状态
                        _isStreaming.value = false
                        _streamingContent.value = ""
                    },
                    onError = { errorMessage ->
                        // 处理错误
                        val errorMsg = ChatMessage(
                            content = "抱歉，发生了错误：$errorMessage",
                            isFromUser = false
                        )
                        val errorMessages = updatedMessages + errorMsg
                        val errorSession = updatedSession.copy(messages = errorMessages)
                        
                        _currentSession.value = errorSession
                        
                        // 即使出错，也要将会话添加到列表中
                        if (!_chatSessions.value.any { it.id == errorSession.id }) {
                            _chatSessions.value = listOf(errorSession) + _chatSessions.value
                        }
                        updateSessionInList(errorSession)
                        
                        // 重置流式状态
                        _isStreaming.value = false
                        _streamingContent.value = ""
                    }
                )
            } else if (hasVisionUnderstanding) {
                // 使用视觉理解的流式输出
                visionUnderstandingService.generateVisionUnderstandingStream(
                    messageHistory = updatedMessages,
                    attachments = selectedAttachments,
                    onContent = { contentChunk ->
                        // 更新流式内容
                        _streamingContent.value += contentChunk
                        
                        // 更新临时消息的内容
                        val currentMessages = _currentSession.value?.messages?.toMutableList() ?: return@generateVisionUnderstandingStream
                        if (currentMessages.isNotEmpty()) {
                            val lastIndex = currentMessages.size - 1
                            currentMessages[lastIndex] = currentMessages[lastIndex].copy(content = _streamingContent.value)
                            val updatedSessionWithStream = _currentSession.value?.copy(messages = currentMessages)
                            _currentSession.value = updatedSessionWithStream
                        }
                    },
                    onComplete = { result ->
                        // 流式输出完成，创建最终的AI消息
                        val finalAiMessage = ChatMessage(content = result.content, isFromUser = false)
                        val finalMessages = updatedMessages + finalAiMessage
                        val finalSession = updatedSession.copy(messages = finalMessages)
                        
                        _currentSession.value = finalSession
                        
                        // 第一段对话完成后，将会话添加到会话列表并保存
                        if (!_chatSessions.value.any { it.id == finalSession.id }) {
                            _chatSessions.value = listOf(finalSession) + _chatSessions.value
                        }
                        updateSessionInList(finalSession)
                        
                        // 保存到本地存储
                        saveSessions()
                        
                        // 重置流式状态
                        _isStreaming.value = false
                        _streamingContent.value = ""
                    },
                    onError = { errorMessage ->
                        // 处理错误
                        val errorMsg = ChatMessage(
                            content = "抱歉，发生了错误：$errorMessage",
                            isFromUser = false
                        )
                        val errorMessages = updatedMessages + errorMsg
                        val errorSession = updatedSession.copy(messages = errorMessages)
                        
                        _currentSession.value = errorSession
                        
                        // 即使出错，也要将会话添加到列表中
                        if (!_chatSessions.value.any { it.id == errorSession.id }) {
                            _chatSessions.value = listOf(errorSession) + _chatSessions.value
                        }
                        updateSessionInList(errorSession)
                        
                        // 重置流式状态
                        _isStreaming.value = false
                        _streamingContent.value = ""
                    }
                )
            } else {
                // 使用普通聊天的流式输出
                chatApi.sendMessageStream(
                    messageHistory = updatedMessages,
                    onStreamContent = { contentChunk ->
                        // 更新流式内容
                        _streamingContent.value += contentChunk
                        
                        // 更新临时消息的内容
                        val currentMessages = _currentSession.value?.messages?.toMutableList() ?: return@sendMessageStream
                        if (currentMessages.isNotEmpty()) {
                            val lastIndex = currentMessages.size - 1
                            currentMessages[lastIndex] = currentMessages[lastIndex].copy(content = _streamingContent.value)
                            val updatedSessionWithStream = _currentSession.value?.copy(messages = currentMessages)
                            _currentSession.value = updatedSessionWithStream
                        }
                    },
                    onComplete = { fullContent ->
                        // 流式输出完成，创建最终的AI消息
                        val finalAiMessage = ChatMessage(content = fullContent, isFromUser = false)
                        val finalMessages = updatedMessages + finalAiMessage
                        val finalSession = updatedSession.copy(messages = finalMessages)
                        
                        _currentSession.value = finalSession
                        
                        // 第一段对话完成后，将会话添加到会话列表并保存
                        if (!_chatSessions.value.any { it.id == finalSession.id }) {
                            _chatSessions.value = listOf(finalSession) + _chatSessions.value
                        }
                        updateSessionInList(finalSession)
                        
                        // 保存到本地存储
                        saveSessions()
                        
                        // 重置流式状态
                        _isStreaming.value = false
                        _streamingContent.value = ""
                    },
                    onError = { errorMessage ->
                        // 处理错误
                        val errorMsg = ChatMessage(
                            content = "抱歉，发生了错误：$errorMessage",
                            isFromUser = false
                        )
                        val errorMessages = updatedMessages + errorMsg
                        val errorSession = updatedSession.copy(messages = errorMessages)
                        
                        _currentSession.value = errorSession
                        
                        // 即使出错，也要将会话添加到列表中
                        if (!_chatSessions.value.any { it.id == errorSession.id }) {
                            _chatSessions.value = listOf(errorSession) + _chatSessions.value
                        }
                        updateSessionInList(errorSession)
                        
                        // 重置流式状态
                        _isStreaming.value = false
                        _streamingContent.value = ""
                    }
                )
            }
        }
    }
    

    
    fun selectSession(session: ChatSession) {
        _currentSession.value = session
    }
    
    fun deleteSession(sessionId: String) {
        // 从内存中删除
        _chatSessions.value = _chatSessions.value.filter { it.id != sessionId }
        // 从本地存储中删除
        repository.deleteSession(sessionId)
        
        // 如果删除的是当前会话，创建新对话
        if (_currentSession.value?.id == sessionId) {
            createNewChat()
        }
    }
    
    fun renameSession(sessionId: String, newTitle: String) {
        val sessions = _chatSessions.value.toMutableList()
        val index = sessions.indexOfFirst { it.id == sessionId }
        if (index != -1) {
            val updatedSession = sessions[index].copy(title = newTitle)
            sessions[index] = updatedSession
            _chatSessions.value = sessions
            
            // 更新当前会话（如果是当前会话）
            if (_currentSession.value?.id == sessionId) {
                _currentSession.value = updatedSession
            }
            
            // 保存到本地存储
            repository.updateSession(updatedSession)
        }
    }
    
    private fun updateSessionInRepository(session: ChatSession) {
        repository.updateSession(session)
    }
    
    private fun updateSessionInList(updatedSession: ChatSession) {
        val sessions = _chatSessions.value.toMutableList()
        val index = sessions.indexOfFirst { it.id == updatedSession.id }
        if (index != -1) {
            // 更新会话标题（使用第一条用户消息作为标题）
            val title = if (updatedSession.messages.isNotEmpty()) {
                val firstUserMessage = updatedSession.messages.firstOrNull { it.isFromUser }
                firstUserMessage?.content?.take(20) ?: "新对话"
            } else {
                "新对话"
            }
            sessions[index] = updatedSession.copy(title = title)
            _chatSessions.value = sessions
        }
    }
}