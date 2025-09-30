package com.glassous.openqwens.utils

import com.glassous.openqwens.data.ChatRepository
import com.glassous.openqwens.data.ChatMessage
import android.content.Context
import java.io.File

object ImageUtils {
    
    /**
     * 获取所有聊天记录中的生成图片
     * @param context 上下文
     * @return 所有图片的路径列表（优先本地路径，找不到再使用网络URL）
     */
    fun getAllGeneratedImages(context: Context): List<String> {
        val repository = ChatRepository(context)
        val sessions = repository.loadSessions()
        val allImages = mutableListOf<String>()
        
        sessions.forEach { session ->
            session.messages.forEach { message ->
                // 优先使用本地图片路径
                val localPaths = message.localImagePaths ?: emptyList()
                val imageUrls = message.imageUrls ?: emptyList()
                
                // 如果有本地路径，优先使用本地路径
                if (localPaths.isNotEmpty()) {
                    // 检查本地文件是否存在
                    localPaths.forEach { localPath ->
                        if (File(localPath).exists()) {
                            allImages.add(localPath)
                        }
                    }
                } else if (imageUrls.isNotEmpty()) {
                    // 如果没有本地路径或本地文件不存在，使用网络URL
                    allImages.addAll(imageUrls)
                }
            }
        }
        
        return allImages.distinct() // 去重
    }
    
    /**
     * 获取所有AI生成的图片（仅包含AI回复消息中的图片）
     * @param context 上下文
     * @return AI生成的图片路径列表（优先本地路径，找不到再使用网络URL）
     */
    fun getAIGeneratedImages(context: Context): List<String> {
        val repository = ChatRepository(context)
        val sessions = repository.loadSessions()
        val aiImages = mutableListOf<String>()
        
        sessions.forEach { session ->
            session.messages.forEach { message ->
                // 只获取AI回复消息中的图片
                if (!message.isFromUser) {
                    val localPaths = message.localImagePaths ?: emptyList()
                    val imageUrls = message.imageUrls ?: emptyList()
                    
                    // 优先使用本地图片路径
                    if (localPaths.isNotEmpty()) {
                        // 检查本地文件是否存在
                        localPaths.forEach { localPath ->
                            if (File(localPath).exists()) {
                                aiImages.add(localPath)
                            }
                        }
                    } else if (imageUrls.isNotEmpty()) {
                        // 如果没有本地路径或本地文件不存在，使用网络URL
                        aiImages.addAll(imageUrls)
                    }
                }
            }
        }
        
        return aiImages.distinct() // 去重
    }
}