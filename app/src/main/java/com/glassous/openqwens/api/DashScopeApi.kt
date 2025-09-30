package com.glassous.openqwens.api

import com.alibaba.dashscope.aigc.generation.Generation
import com.alibaba.dashscope.aigc.generation.GenerationParam
import com.alibaba.dashscope.aigc.generation.GenerationResult
import com.alibaba.dashscope.common.Message
import com.alibaba.dashscope.common.Role
import com.alibaba.dashscope.exception.ApiException
import com.alibaba.dashscope.exception.InputRequiredException
import com.alibaba.dashscope.exception.NoApiKeyException
import com.glassous.openqwens.data.ChatMessage
import com.glassous.openqwens.ui.theme.DashScopeConfigManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Arrays

class DashScopeApi(private val configManager: DashScopeConfigManager) {
    
    private val generation = Generation()
    
    suspend fun sendMessage(userMessage: String): ChatMessage = withContext(Dispatchers.IO) {
        try {
            val apiKey = configManager.apiKey
            if (apiKey.isEmpty()) {
                throw IllegalStateException("API密钥未配置，请在设置中配置API密钥")
            }
            
            val selectedModel = configManager.getSelectedModel()
            if (selectedModel == null) {
                throw IllegalStateException("未选择模型，请在设置中选择一个模型")
            }
            
            // 构建系统消息
            val systemMsg = Message.builder()
                .role(Role.SYSTEM.getValue())
                .content("You are a helpful assistant.")
                .build()
            
            // 构建用户消息
            val userMsg = Message.builder()
                .role(Role.USER.getValue())
                .content(userMessage)
                .build()
            
            // 构建请求参数
            val param = GenerationParam.builder()
                .apiKey(apiKey)
                .model(selectedModel.id)
                .messages(Arrays.asList(systemMsg, userMsg))
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .build()
            
            // 调用API
            val result: GenerationResult = generation.call(param)
            
            // 解析响应
            val choices = result.output?.choices
            if (choices != null && choices.isNotEmpty()) {
                val responseContent = choices[0].message?.content ?: "抱歉，没有收到有效的回复"
                ChatMessage(
                    content = responseContent,
                    isFromUser = false
                )
            } else {
                ChatMessage(
                    content = "抱歉，API返回了空的响应",
                    isFromUser = false
                )
            }
            
        } catch (e: NoApiKeyException) {
            ChatMessage(
                content = "API密钥错误或未配置，请检查设置中的API密钥配置",
                isFromUser = false
            )
        } catch (e: ApiException) {
            ChatMessage(
                content = "API调用失败：${e.message}",
                isFromUser = false
            )
        } catch (e: InputRequiredException) {
            ChatMessage(
                content = "请求参数不完整：${e.message}",
                isFromUser = false
            )
        } catch (e: IllegalStateException) {
            ChatMessage(
                content = e.message ?: "配置错误",
                isFromUser = false
            )
        } catch (e: Exception) {
            ChatMessage(
                content = "发生未知错误：${e.message}",
                isFromUser = false
            )
        }
    }
}