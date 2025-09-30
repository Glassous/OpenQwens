package com.glassous.openqwens.ui.components

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 附件类型枚举
 */
enum class AttachmentType(
    val id: String,
    val displayName: String,
    val description: String,
    val mimeTypePrefix: String
) {
    IMAGE("image", "图片", "图片文件", "image/"),
    VIDEO("video", "视频", "视频文件", "video/"),
    AUDIO("audio", "音频", "音频文件", "audio/"),
    DOCUMENT("document", "文档", "文档文件", "application/"),
    TEXT("text", "文本", "文本文件", "text/"),
    OTHER("other", "其他", "其他文件", "")
}

/**
 * 附件数据类
 * 包含文件的基本信息和base64编码的内容
 */
data class AttachmentData(
    val id: String,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val attachmentType: AttachmentType,
    val base64Content: String,
    val icon: ImageVector,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * 获取格式化的文件大小
     */
    fun getFormattedFileSize(): String {
        return when {
            fileSize < 1024 -> "${fileSize}B"
            fileSize < 1024 * 1024 -> "${fileSize / 1024}KB"
            fileSize < 1024 * 1024 * 1024 -> "${fileSize / (1024 * 1024)}MB"
            else -> "${fileSize / (1024 * 1024 * 1024)}GB"
        }
    }
    
    /**
     * 获取显示用的文件名（处理超长文件名）
     */
    fun getDisplayFileName(maxLength: Int = 20): String {
        return if (fileName.length <= maxLength) {
            fileName
        } else {
            val extension = fileName.substringAfterLast(".", "")
            val nameWithoutExtension = fileName.substringBeforeLast(".")
            val truncatedName = nameWithoutExtension.take(maxLength - extension.length - 4) // 4 for "..." and "."
            if (extension.isNotEmpty()) {
                "$truncatedName...$extension"
            } else {
                "${fileName.take(maxLength - 3)}..."
            }
        }
    }
}

/**
 * 根据MIME类型确定附件类型
 */
fun getMimeTypeAttachmentType(mimeType: String): AttachmentType {
    return when {
        mimeType.startsWith("image/") -> AttachmentType.IMAGE
        mimeType.startsWith("video/") -> AttachmentType.VIDEO
        mimeType.startsWith("audio/") -> AttachmentType.AUDIO
        mimeType.startsWith("text/") -> AttachmentType.TEXT
        mimeType.startsWith("application/pdf") -> AttachmentType.DOCUMENT
        mimeType.startsWith("application/msword") -> AttachmentType.DOCUMENT
        mimeType.startsWith("application/vnd.openxmlformats-officedocument") -> AttachmentType.DOCUMENT
        mimeType.startsWith("application/vnd.ms-excel") -> AttachmentType.DOCUMENT
        mimeType.startsWith("application/vnd.ms-powerpoint") -> AttachmentType.DOCUMENT
        else -> AttachmentType.OTHER
    }
}