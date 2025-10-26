package com.glassous.openqwens.network

import com.alibaba.dashscope.aigc.generation.Generation
import com.alibaba.dashscope.aigc.generation.GenerationParam
import com.alibaba.dashscope.aigc.generation.GenerationResult
import com.alibaba.dashscope.aigc.generation.SearchOptions
import com.alibaba.dashscope.common.Message
import com.alibaba.dashscope.common.Role
import com.alibaba.dashscope.exception.ApiException
import com.alibaba.dashscope.exception.InputRequiredException
import com.alibaba.dashscope.exception.NoApiKeyException
import com.alibaba.dashscope.utils.Constants
import com.glassous.openqwens.data.ChatMessage
import com.glassous.openqwens.ui.theme.DashScopeConfigManager
import io.reactivex.Flowable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class WebSearchService(
    private val configManager: DashScopeConfigManager
) {
    
    companion object {
        // 阿里云百炼API配置
        private const val BASE_URL = "https://dashscope.aliyuncs.com/api/v1"
        // 联网搜索模型
        private const val WEB_SEARCH_MODEL = "qwen-plus"
        
        init {
            // 设置基础URL
            Constants.baseHttpApiUrl = BASE_URL
        }
    }
    
    /**
     * 联网搜索结果数据类
     */
    data class WebSearchResult(
        val content: String,           // 回复内容
        val searchResults: List<SearchResultItem>  // 搜索结果来源
    )
    
    /**
     * 搜索结果项
     */
    data class SearchResultItem(
        val siteName: String,
        val title: String,
        val url: String,
        val icon: String,
        val index: Int
    )
    
    /**
     * 将ChatMessage转换为DashScope的Message格式
     */
    private fun convertToMessage(chatMessage: ChatMessage): Message {
        val role = if (chatMessage.isFromUser) Role.USER else Role.ASSISTANT
        return Message.builder()
            .role(role.getValue())
            .content(chatMessage.content)
            .build()
    }
    
    /**
     * 构建消息列表（包含历史对话）
     */
    private fun buildMessages(messageHistory: List<ChatMessage>): List<Message> {
        return messageHistory.map { convertToMessage(it) }
    }
    
    /**
     * 联网搜索生成
     * @param messageHistory 完整的对话历史记录
     * @return 联网搜索结果
     */
    suspend fun performWebSearch(
        messageHistory: List<ChatMessage>
    ): Result<WebSearchResult> = withContext(Dispatchers.IO) {
        try {
            val apiKey = configManager.apiKey
            if (apiKey.isBlank()) {
                return@withContext Result.failure(Exception("API Key未配置"))
            }
            
            // 构建包含历史对话的消息列表
            val messages = buildMessages(messageHistory)
            
            // 配置搜索选项
            val searchOptions = SearchOptions.builder()
                .forcedSearch(true)       // 强制联网搜索
                .enableSource(true)       // 必须开启才能使用角标标注
                .enableCitation(true)     // 开启角标标注
                .citationFormat("[ref_<number>]") // 设置角标样式
                .build()
            
            // 构建参数
            val param = GenerationParam.builder()
                .apiKey(apiKey)
                .model(configManager.getSelectedModel()?.id ?: WEB_SEARCH_MODEL)
                .messages(messages)
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .enableSearch(true)
                .searchOptions(searchOptions)
                .build()
            
            // 调用API
            val generation = Generation()
            val result: GenerationResult = generation.call(param)
            
            // 解析结果
            val content = result.output?.choices?.get(0)?.message?.content ?: ""
            val searchInfo = result.output?.searchInfo
            val searchResults = mutableListOf<SearchResultItem>()
            
            // 解析搜索结果
            searchInfo?.searchResults?.forEach { searchResult ->
                searchResults.add(
                    SearchResultItem(
                        siteName = searchResult.siteName ?: "未知来源",
                        title = searchResult.title ?: "无标题",
                        url = searchResult.url ?: "",
                        icon = searchResult.icon ?: "",
                        index = searchResult.index ?: 0
                    )
                )
            }
            
            Result.success(
                WebSearchResult(
                    content = content,
                    searchResults = searchResults
                )
            )
            
        } catch (e: ApiException) {
            Result.failure(Exception("API调用失败: ${e.message}"))
        } catch (e: NoApiKeyException) {
            Result.failure(Exception("API Key未配置或无效"))
        } catch (e: InputRequiredException) {
            Result.failure(Exception("输入参数不完整"))
        } catch (e: Exception) {
            Result.failure(Exception("联网搜索失败: ${e.message}"))
        }
    }
    
    /**
     * 为聊天消息生成联网搜索回复
     * @param messageHistory 完整的对话历史记录
     * @return 包含联网搜索结果的聊天消息
     */
    suspend fun performWebSearchForChat(
        messageHistory: List<ChatMessage>
    ): Result<ChatMessage> {
        return try {
            val searchResult = performWebSearch(messageHistory)
            if (searchResult.isSuccess) {
                val result = searchResult.getOrThrow()
                
                // 格式化联网搜索响应
                val formattedContent = formatWebSearchResponse(result.content, result.searchResults)
                
                val assistantMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    content = formattedContent,
                    isFromUser = false,
                    timestamp = System.currentTimeMillis()
                )
                Result.success(assistantMessage)
            } else {
                Result.failure(searchResult.exceptionOrNull() ?: Exception("联网搜索失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 格式化联网搜索响应内容
     */
    private fun formatWebSearchResponse(content: String, searchResults: List<SearchResultItem>): String {
        val builder = StringBuilder()
        
        // 添加搜索结果标识
        builder.append("====================联网搜索结果====================\n")
        
        // 添加搜索来源信息
        if (searchResults.isNotEmpty()) {
            builder.append("**搜索来源：**\n")
            searchResults.forEach { result ->
                builder.append("${result.index}. [${result.title}](${result.url}) - ${result.siteName}\n")
            }
            builder.append("\n")
        }
        
        builder.append("====================回复内容====================\n")
        builder.append(content)
        
        return builder.toString()
    }
    
    /**
     * 流式联网搜索
     * @param messageHistory 完整的对话历史记录
     * @param onContent 内容流式输出回调
     * @param onComplete 完成回调，返回完整的搜索结果
     * @param onError 错误回调
     */
    suspend fun performWebSearchStream(
        messageHistory: List<ChatMessage>,
        onContent: (String) -> Unit,
        onComplete: (WebSearchResult) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val apiKey = configManager.apiKey
            if (apiKey.isBlank()) {
                onError("API Key未配置")
                return@withContext
            }
            
            // 构建包含历史对话的消息列表
            val messages = buildMessages(messageHistory)
            
            // 配置搜索选项
            val searchOptions = SearchOptions.builder()
                .forcedSearch(true)       // 强制联网搜索
                .enableSource(true)       // 必须开启才能使用角标标注
                .enableCitation(true)     // 开启角标标注
                .citationFormat("[ref_<number>]") // 设置角标样式
                .build()
            
            // 构建参数
            val param = GenerationParam.builder()
                .apiKey(apiKey)
                .model(configManager.getSelectedModel()?.id ?: WEB_SEARCH_MODEL)
                .messages(messages)
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .enableSearch(true)
                .searchOptions(searchOptions)
                .incrementalOutput(true)  // 开启增量输出
                .build()
            
            // 调用流式API
            val generation = Generation()
            val result: Flowable<GenerationResult> = generation.streamCall(param)
            
            val contentBuilder = StringBuilder()
            val searchResults = mutableListOf<SearchResultItem>()
            var searchResultsParsed = false
            
            // 处理流式结果
            result.blockingForEach { generationResult ->
                val content = generationResult.output?.choices?.get(0)?.message?.content ?: ""
                
                // 处理内容流式输出
                if (content.isNotEmpty()) {
                    contentBuilder.append(content)
                    onContent(content)
                }
                
                // 解析搜索结果（只在第一次解析）
                if (!searchResultsParsed) {
                    val searchInfo = generationResult.output?.searchInfo
                    searchInfo?.searchResults?.forEach { searchResult ->
                        searchResults.add(
                            SearchResultItem(
                                siteName = searchResult.siteName ?: "未知来源",
                                title = searchResult.title ?: "无标题",
                                url = searchResult.url ?: "",
                                icon = searchResult.icon ?: "",
                                index = searchResult.index ?: 0
                            )
                        )
                    }
                    if (searchResults.isNotEmpty()) {
                        searchResultsParsed = true
                    }
                }
            }
            
            // 完成回调
            val finalResult = WebSearchResult(
                content = contentBuilder.toString(),
                searchResults = searchResults
            )
            onComplete(finalResult)
            
        } catch (e: ApiException) {
            onError("API调用失败: ${e.message}")
        } catch (e: NoApiKeyException) {
            onError("API Key未配置或无效")
        } catch (e: InputRequiredException) {
            onError("输入参数不完整")
        } catch (e: Exception) {
            onError("联网搜索失败: ${e.message}")
        }
    }
    
    /**
     * 为聊天消息生成流式联网搜索回复
     * @param messageHistory 完整的对话历史记录
     * @param onContent 内容流式输出回调
     * @param onComplete 完成回调，返回包含联网搜索结果的消息
     * @param onError 错误回调
     */
    suspend fun performWebSearchForChatStream(
        messageHistory: List<ChatMessage>,
        onContent: (String) -> Unit,
        onComplete: (ChatMessage) -> Unit,
        onError: (String) -> Unit
    ) {
        performWebSearchStream(
            messageHistory = messageHistory,
            onContent = onContent,
            onComplete = { result ->
                try {
                    // 格式化联网搜索响应
                    val formattedContent = formatWebSearchResponse(result.content, result.searchResults)
                    
                    val assistantMessage = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        content = formattedContent,
                        isFromUser = false,
                        timestamp = System.currentTimeMillis()
                    )
                    onComplete(assistantMessage)
                } catch (e: Exception) {
                    onError("格式化搜索结果失败: ${e.message}")
                }
            },
            onError = onError
        )
    }
}