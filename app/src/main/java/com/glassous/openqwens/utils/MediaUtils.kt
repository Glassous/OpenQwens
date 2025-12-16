package com.glassous.openqwens.utils

import com.glassous.openqwens.data.ChatRepository
import com.glassous.openqwens.data.ChatMessage
import android.content.Context
import java.io.File

sealed class MediaItem {
    data class Image(val path: String) : MediaItem()
    data class Video(val path: String) : MediaItem()
    
    val filePath: String
        get() = when (this) {
            is Image -> path
            is Video -> path
        }
}

object MediaUtils {
    
    /**
     * 获取所有AI生成的内容（图片和视频）
     * @param context 上下文
     * @return 媒体内容列表
     */
    fun getGeneratedMedia(context: Context): List<MediaItem> {
        val repository = ChatRepository(context)
        val sessions = repository.loadSessions()
        val mediaItems = mutableListOf<MediaItem>()
        
        sessions.forEach { session ->
            session.messages.forEach { message ->
                // 只获取AI回复消息中的内容
                if (!message.isFromUser) {
                    // 处理图片
                    val localPaths = message.localImagePaths ?: emptyList()
                    val imageUrls = message.imageUrls ?: emptyList()
                    
                    if (localPaths.isNotEmpty()) {
                        localPaths.forEach { localPath ->
                            if (File(localPath).exists()) {
                                mediaItems.add(MediaItem.Image(localPath))
                            }
                        }
                    } else if (imageUrls.isNotEmpty()) {
                        imageUrls.forEach { url ->
                            mediaItems.add(MediaItem.Image(url))
                        }
                    }
                    
                    // 处理视频
                    if (!message.localVideoPath.isNullOrBlank() && File(message.localVideoPath).exists()) {
                        mediaItems.add(MediaItem.Video(message.localVideoPath))
                    } else if (!message.videoUrl.isNullOrBlank()) {
                        mediaItems.add(MediaItem.Video(message.videoUrl))
                    }
                }
            }
        }
        
        return mediaItems.distinctBy { it.filePath }
    }
}
