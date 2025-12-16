package com.glassous.openqwens.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.util.UUID

class MediaDownloadManager(private val context: Context) {
    
    private val videosDir: File by lazy {
        File(context.filesDir, "chat_videos").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }
    
    /**
     * 下载视频并保存到本地
     * @param videoUrl 视频URL
     * @return 本地文件路径，如果下载失败返回null
     */
    suspend fun downloadVideo(videoUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            // 生成唯一的文件名
            val fileName = "${UUID.randomUUID()}.mp4"
            val localFile = File(videosDir, fileName)
            
            // 下载视频
            val url = URL(videoUrl)
            val connection = url.openConnection()
            connection.connect()
            
            val inputStream = connection.getInputStream()
            val outputStream = FileOutputStream(localFile)
            
            val buffer = ByteArray(4096)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            
            outputStream.close()
            inputStream.close()
            
            localFile.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 删除本地视频文件
     * @param localPath 本地文件路径
     */
    fun deleteLocalVideo(localPath: String) {
        try {
            val file = File(localPath)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
