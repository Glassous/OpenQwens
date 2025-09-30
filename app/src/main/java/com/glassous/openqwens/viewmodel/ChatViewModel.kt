package com.glassous.openqwens.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.glassous.openqwens.api.DashScopeApi
import com.glassous.openqwens.data.ChatMessage
import com.glassous.openqwens.data.ChatRepository
import com.glassous.openqwens.data.ChatSession
import com.glassous.openqwens.ui.theme.DashScopeConfigManager
import com.glassous.openqwens.ui.theme.GlobalDashScopeConfigManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    
    private val configManager = GlobalDashScopeConfigManager.getInstance(application)
    private val chatApi = DashScopeApi(configManager)
    private val repository = ChatRepository(application)
    
    private val _currentSession = MutableStateFlow<ChatSession?>(null)
    val currentSession: StateFlow<ChatSession?> = _currentSession.asStateFlow()
    
    private val _chatSessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val chatSessions: StateFlow<List<ChatSession>> = _chatSessions.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        // 加载保存的聊天记录
        loadSavedSessions()
        // 应用启动时总是创建新对话
        createNewChat()
    }
    
    private fun loadSavedSessions() {
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
    
    fun sendMessage(content: String) {
        val currentSession = _currentSession.value ?: return
        
        // 立即设置加载状态为true，显示加载动画
        _isLoading.value = true
        
        // 添加用户消息
        val userMessage = ChatMessage(content = content, isFromUser = true)
        val updatedMessages = currentSession.messages + userMessage
        val updatedSession = currentSession.copy(messages = updatedMessages)
        
        _currentSession.value = updatedSession
        updateSessionInList(updatedSession)
        
        // 发送到API并获取回复（传递完整的消息历史）
        viewModelScope.launch {
            try {
                // 传递完整的消息历史记录给API，实现多轮对话
                val response = chatApi.sendMessage(updatedMessages)
                // 只有在API响应成功后才添加AI回复消息
                val finalMessages = updatedMessages + response
                val finalSession = updatedSession.copy(messages = finalMessages)
                
                _currentSession.value = finalSession
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
                updateSessionInList(errorSession)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun createNewChat() {
        // 检查当前会话是否为空，避免创建重复的空记录
        val currentSession = _currentSession.value
        if (currentSession != null && currentSession.messages.isEmpty()) {
            // 如果当前会话已经是空的，不需要创建新的
            return
        }
        
        val newSession = ChatSession(title = "新对话")
        _currentSession.value = newSession
        _chatSessions.value = listOf(newSession) + _chatSessions.value
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