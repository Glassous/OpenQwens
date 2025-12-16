package com.glassous.openqwens.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.glassous.openqwens.ui.components.AttachmentData
import com.glassous.openqwens.ui.components.AttachmentType
import com.glassous.openqwens.ui.components.getMimeTypeAttachmentType
import com.glassous.openqwens.ui.components.getAttachmentIcon
import java.io.InputStream
import java.util.UUID

/**
 * 文件处理工具类
 * 提供文件选择、base64编码等功能
 */
object FileUtils {
    
    /**
     * 从URI创建AttachmentData对象
     * 包含base64编码处理
     */
    fun createAttachmentFromUri(
        context: Context,
        uri: Uri
    ): AttachmentData? {
        return try {
            val contentResolver = context.contentResolver
            
            // 获取文件信息
            val cursor = contentResolver.query(uri, null, null, null, null)
            var fileName = "unknown_file"
            var fileSize = 0L
            
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                    
                    if (nameIndex != -1) {
                        fileName = it.getString(nameIndex) ?: "unknown_file"
                    }
                    if (sizeIndex != -1) {
                        fileSize = it.getLong(sizeIndex)
                    }
                }
            }
            
            // 获取MIME类型
            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
            val attachmentType = getMimeTypeAttachmentType(mimeType)
            
            // 读取文件内容并转换为base64
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val base64Content = inputStream?.use { stream ->
                val bytes = stream.readBytes()
                Base64.encodeToString(bytes, Base64.DEFAULT)
            } ?: ""
            
            // 获取对应的图标
            val icon = getAttachmentIcon(attachmentType)
            
            AttachmentData(
                id = UUID.randomUUID().toString(),
                fileName = fileName,
                fileSize = fileSize,
                mimeType = mimeType,
                attachmentType = attachmentType,
                base64Content = base64Content,
                uri = uri.toString(),
                icon = icon
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 检查文件大小是否在允许范围内
     * @param fileSize 文件大小（字节）
     * @param maxSizeMB 最大允许大小（MB）
     */
    fun isFileSizeValid(fileSize: Long, maxSizeMB: Int = 10): Boolean {
        val maxSizeBytes = maxSizeMB * 1024 * 1024L
        return fileSize <= maxSizeBytes
    }
    
    /**
     * 检查文件类型是否被支持
     */
    fun isMimeTypeSupported(mimeType: String): Boolean {
        val supportedTypes = listOf(
            // 图片
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp"
        )
        
        return supportedTypes.any { mimeType.startsWith(it) } || 
               mimeType.startsWith("image/")
    }
    
    /**
     * 获取文件扩展名
     */
    fun getFileExtension(fileName: String): String {
        return fileName.substringAfterLast(".", "")
    }
    
    /**
     * 根据文件扩展名推断MIME类型
     */
    fun getMimeTypeFromExtension(extension: String): String {
        return when (extension.lowercase()) {
            // 图片
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "bmp" -> "image/bmp"
            
            // 视频
            "mp4" -> "video/mp4"
            "avi" -> "video/avi"
            "mov" -> "video/mov"
            "wmv" -> "video/wmv"
            "flv" -> "video/flv"
            
            // 音频
            "mp3" -> "audio/mp3"
            "wav" -> "audio/wav"
            "aac" -> "audio/aac"
            "ogg" -> "audio/ogg"
            "m4a" -> "audio/m4a"
            
            // 文档
            "pdf" -> "application/pdf"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "ppt" -> "application/vnd.ms-powerpoint"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            
            // 文本
            "txt" -> "text/plain"
            "html", "htm" -> "text/html"
            "css" -> "text/css"
            "js" -> "text/javascript"
            "json" -> "application/json"
            
            else -> "application/octet-stream"
        }
    }
}