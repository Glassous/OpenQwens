package com.glassous.openqwens.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ChatRepository(context: Context) {
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("chat_sessions", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val SESSIONS_KEY = "chat_sessions"
    }
    
    fun saveSessions(sessions: List<ChatSession>) {
        // 过滤掉空对话（没有消息的会话）
        val nonEmptySessions = sessions.filter { it.messages.isNotEmpty() }
        val json = gson.toJson(nonEmptySessions)
        sharedPreferences.edit()
            .putString(SESSIONS_KEY, json)
            .apply()
    }
    
    fun loadSessions(): List<ChatSession> {
        val json = sharedPreferences.getString(SESSIONS_KEY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<ChatSession>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun deleteSession(sessionId: String) {
        val sessions = loadSessions().filter { it.id != sessionId }
        saveSessions(sessions)
    }
    
    fun updateSession(updatedSession: ChatSession) {
        val sessions = loadSessions().toMutableList()
        val index = sessions.indexOfFirst { it.id == updatedSession.id }
        if (index != -1) {
            sessions[index] = updatedSession
            saveSessions(sessions)
        }
    }
}