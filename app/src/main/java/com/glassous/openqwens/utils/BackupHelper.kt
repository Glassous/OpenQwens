package com.glassous.openqwens.utils

import android.content.Context
import android.net.Uri
import com.glassous.openqwens.data.ChatRepository
import com.glassous.openqwens.data.ChatSession
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class BackupHelper(private val context: Context) {
    private val repository = ChatRepository(context)
    private val gson = Gson()

    suspend fun exportData(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val sessions = repository.loadSessions()
            val json = gson.toJson(sessions)
            
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(json.toByteArray())
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importData(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val json = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).readText()
            } ?: return@withContext Result.failure(Exception("无法读取文件"))

            val type = object : TypeToken<List<ChatSession>>() {}.type
            val importedSessions: List<ChatSession> = gson.fromJson(json, type)

            if (importedSessions.isEmpty()) {
                return@withContext Result.success(0)
            }

            val existingSessions = repository.loadSessions().toMutableList()
            var addedCount = 0
            var updatedCount = 0
            
            for (session in importedSessions) {
                val index = existingSessions.indexOfFirst { it.id == session.id }
                if (index != -1) {
                    existingSessions[index] = session
                    updatedCount++
                } else {
                    existingSessions.add(session)
                    addedCount++
                }
            }
            
            repository.saveSessions(existingSessions)
            Result.success(addedCount + updatedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
