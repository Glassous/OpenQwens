package com.glassous.openqwens.network

import com.alibaba.dashscope.aigc.videosynthesis.VideoSynthesis
import com.alibaba.dashscope.aigc.videosynthesis.VideoSynthesisParam
import com.alibaba.dashscope.aigc.videosynthesis.VideoSynthesisResult
import com.alibaba.dashscope.exception.ApiException
import com.alibaba.dashscope.exception.NoApiKeyException
import com.alibaba.dashscope.utils.Constants
import com.glassous.openqwens.data.ChatMessage
import com.glassous.openqwens.ui.theme.DashScopeConfigManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

import com.glassous.openqwens.data.VideoGenerationParams

class VideoGenerationService(
    private val configManager: DashScopeConfigManager
) {

    companion object {
        // 阿里云百炼API配置
        private const val BASE_URL = "https://dashscope.aliyuncs.com/api/v1"
        private const val DEFAULT_MODEL = "wanx-v1" // 假设默认模型为 wanx-v1

        init {
            // 设置基础URL
            Constants.baseHttpApiUrl = BASE_URL
        }
    }

    /**
     * 生成视频
     * @param prompt 视频描述提示词
     * @param imageUrl 图片URL（可选，用于图生视频）
     * @param params 视频生成参数
     * @return 生成的视频URL
     */
    suspend fun generateVideo(
        prompt: String,
        imageUrl: String? = null,
        params: VideoGenerationParams
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = configManager.apiKey
            if (apiKey.isBlank()) {
                return@withContext Result.failure(Exception("API Key未配置"))
            }

            // 构建parameters
            val parameters = hashMapOf<String, Any>(
                "resolution" to params.resolution,
                "duration" to params.duration
            )

            // 构建参数
            val paramBuilder = VideoSynthesisParam.builder()
                .apiKey(apiKey)
                .model(configManager.getSelectedModel()?.id ?: DEFAULT_MODEL)
                .prompt(prompt)
                .parameters(parameters)
            
            if (imageUrl != null) {
                paramBuilder.imgUrl(imageUrl)
            }
                
            val param = paramBuilder.build()
            val synthesis = VideoSynthesis()
            
            // 调用API
            // 注意：视频生成通常是异步的，SDK可能封装了轮询，或者直接返回Result包含Job ID
            // 这里假设SDK提供了同步调用的call方法，或者call方法内部处理了轮询
            // 如果是异步，通常需要 call -> getTask -> wait -> getResult
            // ImageSynthesis.call 是同步等待结果的 (或者阻塞直到完成)
            val result: VideoSynthesisResult = synthesis.call(param)
            
            // 解析结果
            // VideoSynthesisResult 结构通常包含 output，output包含 videoUrl
            if (result.output != null && result.output.videoUrl != null) {
                Result.success(result.output.videoUrl)
            } else {
                Result.failure(Exception("未能生成视频，返回结果为空"))
            }
            
        } catch (e: ApiException) {
            Result.failure(Exception("API调用失败: ${e.message}"))
        } catch (e: NoApiKeyException) {
            Result.failure(Exception("API Key未配置或无效"))
        } catch (e: Exception) {
            Result.failure(Exception("视频生成失败: ${e.message}"))
        }
    }

    /**
     * 为聊天消息生成视频
     * @param userMessage 用户的消息
     * @param prompt 视频生成提示词
     * @param imageUrl 参考图片URL（可选）
     * @param params 视频生成参数
     * @return 包含生成视频的助手回复消息
     */
    suspend fun generateVideoForChat(
        userMessage: ChatMessage,
        prompt: String,
        imageUrl: String? = null,
        params: VideoGenerationParams
    ): Result<ChatMessage> {
        return try {
            val videoResult = generateVideo(prompt, imageUrl, params)

            if (videoResult.isSuccess) {
                val videoUrl = videoResult.getOrNull()

                // 创建包含视频的回复消息
                val assistantMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    content = "我为您生成了视频：",
                    isFromUser = false,
                    timestamp = System.currentTimeMillis(),
                    videoUrl = videoUrl,
                    isImageGeneration = false // 视频生成不是图片生成
                )

                Result.success(assistantMessage)
            } else {
                val errorMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    content = "抱歉，视频生成失败：${videoResult.exceptionOrNull()?.message}",
                    isFromUser = false,
                    timestamp = System.currentTimeMillis(),
                    videoUrl = null
                )
                Result.success(errorMessage)
            }
        } catch (e: Exception) {
            val errorMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                content = "视频生成过程中发生错误：${e.message}",
                isFromUser = false,
                timestamp = System.currentTimeMillis(),
                videoUrl = null
            )
            Result.success(errorMessage)
        }
    }
}
