package com.glassous.openqwens.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.util.UUID

class ImageDownloadManager(private val context: Context) {
    
    private val imagesDir: File by lazy {
        File(context.filesDir, "chat_images").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }
    
    /**
     * 下载图片并保存到本地
     * @param imageUrl 图片URL
     * @return 本地文件路径，如果下载失败返回null
     */
    suspend fun downloadImage(imageUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            // 生成唯一的文件名
            val fileName = "${UUID.randomUUID()}.png"
            val localFile = File(imagesDir, fileName)
            
            // 下载图片
            val url = URL(imageUrl)
            val connection = url.openConnection()
            connection.doInput = true
            connection.connect()
            
            val inputStream = connection.getInputStream()
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            
            if (bitmap != null) {
                // 保存到本地文件
                val outputStream = FileOutputStream(localFile)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.close()
                bitmap.recycle()
                
                localFile.absolutePath
            } else {
                null
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 批量下载图片
     * @param imageUrls 图片URL列表
     * @return 本地文件路径列表
     */
    suspend fun downloadImages(imageUrls: List<String>): List<String> = withContext(Dispatchers.IO) {
        imageUrls.mapNotNull { url ->
            downloadImage(url)
        }
    }
    
    /**
     * 删除本地图片文件
     * @param localPath 本地文件路径
     */
    fun deleteLocalImage(localPath: String) {
        try {
            val file = File(localPath)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 清理所有本地图片缓存
     */
    fun clearImageCache() {
        try {
            imagesDir.listFiles()?.forEach { file ->
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}