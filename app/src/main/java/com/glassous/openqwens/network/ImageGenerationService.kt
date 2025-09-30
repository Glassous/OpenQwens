package com.glassous.openqwens.network

import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesis
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisParam
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisResult
import com.alibaba.dashscope.exception.ApiException
import com.alibaba.dashscope.exception.NoApiKeyException
import com.alibaba.dashscope.utils.Constants
import com.glassous.openqwens.data.ChatMessage
import com.glassous.openqwens.ui.theme.DashScopeConfigManager
import com.glassous.openqwens.utils.ImageDownloadManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class ImageGenerationService(
    private val configManager: DashScopeConfigManager,
    private val imageDownloadManager: ImageDownloadManager
) {
    
    companion object {
        // 阿里云百炼API配置
        private const val BASE_URL = "https://dashscope.aliyuncs.com/api/v1"
        private const val MODEL_NAME = "qwen-image-plus"
        private const val DEFAULT_SIZE = "1328*1328"
        
        init {
            // 设置基础URL
            Constants.baseHttpApiUrl = BASE_URL
        }
    }

    /**
     * 生成图片
     * @param prompt 图片描述提示词
     * @param size 图片尺寸，默认为1328*1328
     * @param count 生成图片数量，默认为1
     * @return 生成的图片URL列表
     */
    suspend fun generateImage(
        prompt: String,
        size: String = DEFAULT_SIZE,
        count: Int = 1
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val apiKey = configManager.apiKey
            if (apiKey.isBlank()) {
                return@withContext Result.failure(Exception("API Key未配置"))
            }
            
            // 构建参数
            val parameters = hashMapOf<String, Any>(
                "prompt_extend" to true,  // 启用提示词扩展
                "watermark" to true       // 添加水印
            )
            
            val param = ImageSynthesisParam.builder()
                .apiKey(apiKey)
                .model(MODEL_NAME)
                .prompt(prompt)
                .n(count)
                .size(size)
                .parameters(parameters)
                .build()
            
            // 调用API
            val imageSynthesis = ImageSynthesis()
            val result: ImageSynthesisResult = imageSynthesis.call(param)
            
            // 解析结果
            val imageUrls = mutableListOf<String>()
            result.output?.results?.forEach { imageResult ->
                imageResult["url"]?.let { url ->
                    imageUrls.add(url)
                }
            }
            
            if (imageUrls.isNotEmpty()) {
                Result.success(imageUrls)
            } else {
                Result.failure(Exception("未能生成图片"))
            }
            
        } catch (e: ApiException) {
            Result.failure(Exception("API调用失败: ${e.message}"))
        } catch (e: NoApiKeyException) {
            Result.failure(Exception("API Key未配置或无效"))
        } catch (e: Exception) {
            Result.failure(Exception("图片生成失败: ${e.message}"))
        }
    }

    /**
     * 为聊天消息生成图片
     * @param userMessage 用户的消息
     * @param prompt 图片生成提示词
     * @return 包含生成图片的助手回复消息
     */
    suspend fun generateImageForChat(
        userMessage: ChatMessage,
        prompt: String
    ): Result<ChatMessage> {
        return try {
            val imageResult = generateImage(prompt)
            
            if (imageResult.isSuccess) {
                val imageUrls = imageResult.getOrNull() ?: emptyList()
                
                // 下载图片到本地
                val localPaths = imageDownloadManager.downloadImages(imageUrls)
                
                // 创建包含图片的回复消息
                val assistantMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    content = "我为您生成了图片：",
                    isFromUser = false,
                    timestamp = System.currentTimeMillis(),
                    imageUrls = imageUrls,
                    isImageGeneration = true,
                    localImagePaths = localPaths
                )
                
                Result.success(assistantMessage)
            } else {
                val errorMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    content = "抱歉，图片生成失败：${imageResult.exceptionOrNull()?.message}",
                    isFromUser = false,
                    timestamp = System.currentTimeMillis(),
                    imageUrls = emptyList(),
                    isImageGeneration = false,
                    localImagePaths = emptyList()
                )
                Result.success(errorMessage)
            }
        } catch (e: Exception) {
            val errorMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                content = "图片生成过程中发生错误：${e.message}",
                isFromUser = false,
                timestamp = System.currentTimeMillis(),
                imageUrls = emptyList(),
                isImageGeneration = false,
                localImagePaths = emptyList()
            )
            Result.success(errorMessage)
        }
    }
}