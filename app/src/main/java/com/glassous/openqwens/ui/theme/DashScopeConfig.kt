package com.glassous.openqwens.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 模型分组枚举
 */
enum class ModelGroup(val displayName: String) {
    TEXT("文本"),
    IMAGE("图片生成"),
    VIDEO("视频生成")
}

/**
 * API Key 数据类
 */
data class ApiKey(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val key: String
)

/**
 * 阿里云百炼模型配置数据类
 */
data class DashScopeModel(
    val id: String,
    val name: String,
    val description: String = "",
    val group: ModelGroup = ModelGroup.TEXT,
    val note: String = ""
)

/**
 * 阿里云百炼配置管理器
 */
class DashScopeConfigManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("dashscope_config", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    // 默认基础URL
    private val defaultBaseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
    
    // 配置状态
    private var _baseUrl by mutableStateOf(loadBaseUrl())
    private var _apiKeys by mutableStateOf(loadApiKeys())
    private var _selectedApiKeyId by mutableStateOf(loadSelectedApiKeyId())
    private var _models by mutableStateOf(loadModels())
    private var _selectedModelId by mutableStateOf(loadSelectedModelId())

    // 分组模型选择偏好
    private var _selectedTextModelId by mutableStateOf(loadSelectedModelIdForGroup(ModelGroup.TEXT))
    private var _selectedImageModelId by mutableStateOf(loadSelectedModelIdForGroup(ModelGroup.IMAGE))
    private var _selectedVideoModelId by mutableStateOf(loadSelectedModelIdForGroup(ModelGroup.VIDEO))
    
    init {
        // 迁移旧的API Key
        val oldKey = prefs.getString("api_key", "")
        if (!oldKey.isNullOrEmpty() && _apiKeys.isEmpty()) {
            val newKey = ApiKey(name = "默认API Key", key = oldKey)
            addApiKey(newKey)
            setSelectedApiKey(newKey.id)
            // 清除旧的key以避免重复迁移
            prefs.edit().remove("api_key").apply()
        }
    }

    val baseUrl: String get() = _baseUrl
    val apiKeys: List<ApiKey> get() = _apiKeys
    val selectedApiKeyId: String get() = _selectedApiKeyId
    val apiKey: String get() = _apiKeys.find { it.id == _selectedApiKeyId }?.key ?: ""
    val models: List<DashScopeModel> get() = _models
    val selectedModelId: String get() = _selectedModelId
    
    /**
     * 加载基础URL
     */
    private fun loadBaseUrl(): String {
        return prefs.getString("base_url", defaultBaseUrl) ?: defaultBaseUrl
    }
    
    /**
     * 加载API Keys
     */
    private fun loadApiKeys(): List<ApiKey> {
        val json = prefs.getString("api_keys", "[]") ?: "[]"
        val type = object : TypeToken<List<ApiKey>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 加载选中的API Key ID
     */
    private fun loadSelectedApiKeyId(): String {
        return prefs.getString("selected_api_key_id", "") ?: ""
    }
    
    /**
     * 加载选中的模型ID
     */
    private fun loadSelectedModelId(): String {
        return prefs.getString("selected_model_id", "") ?: ""
    }

    /**
     * 加载指定分组选中的模型ID
     */
    private fun loadSelectedModelIdForGroup(group: ModelGroup): String {
        return prefs.getString("selected_model_id_${group.name}", "") ?: ""
    }
    
    /**
     * 加载所有模型
     */
    private fun loadModels(): List<DashScopeModel> {
        val modelsJson = prefs.getString("models", "[]") ?: "[]"
        val type = object : TypeToken<List<DashScopeModel>>() {}.type
        try {
            val list: List<DashScopeModel>? = gson.fromJson(modelsJson, type)
            // 确保group字段有值（处理旧数据）
            var currentModels = list?.map { 
                if (it.group == null) it.copy(group = ModelGroup.TEXT) else it 
            }?.toMutableList() ?: mutableListOf()

            // 获取最新的预设模型
            val defaultModels = getDefaultModels()
            var hasChanges = false
            
            // 检查是否包含核心模型，如果不包含则添加默认模型
            defaultModels.forEach { defaultModel ->
                if (currentModels.none { it.id == defaultModel.id }) {
                    currentModels.add(defaultModel)
                    hasChanges = true
                }
            }

            if (hasChanges) {
                saveModels(currentModels)
                return currentModels
            }

            return currentModels
        } catch (e: Exception) {
            return getDefaultModels().also { saveModels(it) }
        }
    }

    /**
     * 重置为默认模型
     */
    fun resetToDefaultModels() {
        val defaultModels = getDefaultModels()
        saveModels(defaultModels)
        _models = defaultModels
    }

    private fun getDefaultModels(): List<DashScopeModel> {
        return listOf(
            // 文字模型
            DashScopeModel("qwen3-max", "qwen3-max", description = "Qwen3 Max", group = ModelGroup.TEXT, note = "输入0.0032元/每千tokens\n输出0.0128元/每千tokens"),
            DashScopeModel("qwen3-max-preview", "qwen3-max-preview", description = "Qwen3 Max Preview", group = ModelGroup.TEXT, note = "输入0.006元/每千tokens\n输出0.024元/每千tokens"),
            DashScopeModel("qwen3-235b-a22b-instruct-2507", "qwen3-235b-a22b-instruct-2507", description = "Qwen3 235b A22b Instruct 2507", group = ModelGroup.TEXT, note = "输入0.002元/每千tokens\n输出0.008元/每千tokens"),
            DashScopeModel("qwen3-235b-a22b-thinking-2507", "qwen3-235b-a22b-thinking-2507", description = "Qwen3 235b A22b Thinking 2507", group = ModelGroup.TEXT, note = "输入（思考）0.002元/每千tokens\n输出（思考）0.02元/每千tokens"),
            DashScopeModel("qwen3-vl-235b-a22b-instruct", "qwen3-vl-235b-a22b-instruct", description = "Qwen3 Vl 235b A22b Instruct", group = ModelGroup.TEXT, note = "输入 0.002元/每千tokens\n输出0.008元/每千tokens"),
            DashScopeModel("qwen3-vl-235b-a22b-thinking", "qwen3-vl-235b-a22b-thinking", description = "Qwen3 Vl 235b A22b Thinking", group = ModelGroup.TEXT, note = "输入（思考）0.002元/每千tokens\n输出（思考）0.02元/每千tokens"),
            DashScopeModel("qwen-plus", "qwen-plus", description = "Qwen Plus", group = ModelGroup.TEXT, note = "输入0.0008元/每tokens\n输出0.002元/每千tokens"),
            DashScopeModel("qwen-plus-2025-12-01", "qwen-plus-2025-12-01", description = "Qwen Plus 2025 12 01", group = ModelGroup.TEXT, note = "输入0.0008元/每tokens\n输出0.002元/每千tokens"),
            DashScopeModel("qwen-flash", "qwen-flash", description = "Qwen Flash", group = ModelGroup.TEXT, note = "输入0.00015元/每千tokens\n输出0.0015元/每千tokens"),
            DashScopeModel("qwen-flash-2025-07-28", "qwen-flash-2025-07-28", description = "Qwen Flash 2025 07 28", group = ModelGroup.TEXT, note = "输入0.00015元/每千tokens\n输出0.0015元/每千tokens"),
            DashScopeModel("qwen3-coder-plus", "qwen3-coder-plus", description = "Qwen3 Coder Plus", group = ModelGroup.TEXT, note = "输入0.004元/每千tokens\n输出0.016元/每千tokens"),
            DashScopeModel("qwen3-coder-plus-2025-09-23", "qwen3-coder-plus-2025-09-23", description = "Qwen3 Coder Plus 2025 09 23", group = ModelGroup.TEXT, note = "输入0.004元/每千tokens\n输出0.016元/每千tokens"),
            DashScopeModel("deepseek-v3.2", "deepseek-v3.2", description = "Deepseek V3.2", group = ModelGroup.TEXT, note = "输入0.002元/每千tokens\n输出0.003元/每千tokens"),
            DashScopeModel("deepseek-v3.1", "deepseek-v3.1", description = "Deepseek V3.1", group = ModelGroup.TEXT, note = "输入0.004元/每tokens\n输出0.012元/每千tokens"),
            DashScopeModel("glm-4.6", "glm-4.6", description = "Glm 4.6", group = ModelGroup.TEXT, note = "输入0.003元/每千tokens\n输出0.014元/每千tokens"),
            DashScopeModel("glm-4.5", "glm-4.5", description = "Glm 4.5", group = ModelGroup.TEXT, note = "输入0.003元/每千tokens\n输出0.014元/每千tokens"),
            DashScopeModel("glm-4.5-air", "glm-4.5-air", description = "Glm 4.5 Air", group = ModelGroup.TEXT, note = "输入0.0008元/每千tokens\n输出0.006元/每千tokens"),
            DashScopeModel("kimi-k2-thinking", "kimi-k2-thinking", description = "Kimi K2 Thinking", group = ModelGroup.TEXT, note = "输入0.004元/每千tokens\n输出 0.016元/每千tokens"),
            DashScopeModel("Moonshot-Kimi-K2-Instruct", "Moonshot-Kimi-K2-Instruct", description = "Moonshot Kimi K2 Instruct", group = ModelGroup.TEXT, note = "输入0.004元/每千tokens\n输出0.016元/每千tokens"),

            // 图片生成模型
            DashScopeModel("qwen-image-plus", "qwen-image-plus", description = "Qwen Image Plus", group = ModelGroup.IMAGE, note = "图片生成0.2元/每张"),
            DashScopeModel("qwen-image", "qwen-image", description = "Qwen Image", group = ModelGroup.IMAGE, note = "图片生成0.25元/每张"),
            DashScopeModel("qwen-image-edit-plus", "qwen-image-edit-plus", description = "Qwen Image Edit Plus", group = ModelGroup.IMAGE, note = "图片生成0.2元/每张"),
            DashScopeModel("qwen-image-edit", "qwen-image-edit", description = "Qwen Image Edit", group = ModelGroup.IMAGE, note = "图片生成0.3元/每张"),
            DashScopeModel("qwen-image-edit-plus-2025-12-15", "qwen-image-edit-plus-2025-12-15", description = "Qwen Image Edit Plus 2025 12 15", group = ModelGroup.IMAGE, note = "图片生成0.2元/每张"),
            DashScopeModel("wan2.6-t2i", "wan2.6-t2i", description = "Wan2.6 T2i", group = ModelGroup.IMAGE, note = "图片生成0.2元/每张"),
            DashScopeModel("wan2.5-t2i-preview", "wan2.5-t2i-preview", description = "Wan2.5 T2i Preview", group = ModelGroup.IMAGE, note = "图片生成0.2元/每张"),
            DashScopeModel("wan2.6-image", "wan2.6-image", description = "Wan2.6 Image", group = ModelGroup.IMAGE, note = "图片生成0.2元/每张"),
            DashScopeModel("wan2.5-i2i-preview", "wan2.5-i2i-preview", description = "Wan2.5 I2i Preview", group = ModelGroup.IMAGE, note = "图片生成0.2元/每张"),

            // 视频生成模型
            DashScopeModel("wan2.6-i2v", "wan2.6-i2v", description = "Wan2.6 I2v", group = ModelGroup.VIDEO, note = "（720P）0.6元/每秒\n（1080P）1元/每秒"),
            DashScopeModel("wan2.5-i2v-preview", "wan2.5-i2v-preview", description = "Wan2.5 I2v Preview", group = ModelGroup.VIDEO, note = "（480P）0.3元/每秒\n（720P）0.6元/生成\n（1080P）1元/每秒"),
            DashScopeModel("wan2.6-r2v", "wan2.6-r2v", description = "Wan2.6 R2v", group = ModelGroup.VIDEO, note = "（720P）0.6元/每秒\n（1080P）1元/每秒"),
            DashScopeModel("wan2.6-t2v", "wan2.6-t2v", description = "Wan2.6 T2v", group = ModelGroup.VIDEO, note = "（720P）0.6元/每秒\n（1080P）1元/每秒"),
            DashScopeModel("wan2.5-t2v-preview", "wan2.5-t2v-preview", description = "Wan2.5 T2v Preview", group = ModelGroup.VIDEO, note = "（480P） 0.3元/每秒\n（720P）0.6元/每秒\n（1080P）1元/每秒")
        )
    }
    
    /**
     * 设置基础URL
     */
    fun setBaseUrl(url: String) {
        _baseUrl = url
        prefs.edit().putString("base_url", url).apply()
    }
    
    /**
     * 添加API Key
     */
    fun addApiKey(apiKey: ApiKey) {
        val current = _apiKeys.toMutableList()
        current.add(apiKey)
        _apiKeys = current
        saveApiKeys(current)
        
        // 如果是第一个key，自动选中
        if (current.size == 1) {
            setSelectedApiKey(apiKey.id)
        }
    }

    /**
     * 删除API Key
     */
    fun removeApiKey(id: String) {
        val current = _apiKeys.toMutableList()
        current.removeAll { it.id == id }
        _apiKeys = current
        saveApiKeys(current)
        
        if (_selectedApiKeyId == id) {
            _selectedApiKeyId = ""
            prefs.edit().putString("selected_api_key_id", "").apply()
            // 如果还有其他key，选中第一个
            if (current.isNotEmpty()) {
                setSelectedApiKey(current[0].id)
            }
        }
    }

    /**
     * 更新API Key
     */
    fun updateApiKey(apiKey: ApiKey) {
        val current = _apiKeys.toMutableList()
        val index = current.indexOfFirst { it.id == apiKey.id }
        if (index != -1) {
            current[index] = apiKey
            _apiKeys = current
            saveApiKeys(current)
        }
    }

    /**
     * 设置选中的API Key
     */
    fun setSelectedApiKey(id: String) {
        _selectedApiKeyId = id
        prefs.edit().putString("selected_api_key_id", id).apply()
    }

    private fun saveApiKeys(list: List<ApiKey>) {
        val json = gson.toJson(list)
        prefs.edit().putString("api_keys", json).apply()
    }

    // 兼容旧方法，但现在应该使用addApiKey/updateApiKey
    fun setApiKey(key: String) {
        // 如果当前有选中的key，更新它；否则创建一个新的默认key
        if (_selectedApiKeyId.isNotEmpty()) {
            val currentKey = _apiKeys.find { it.id == _selectedApiKeyId }
            if (currentKey != null) {
                updateApiKey(currentKey.copy(key = key))
                return
            }
        }
        
        // 创建新的默认key
        val newKey = ApiKey(name = "默认API Key", key = key)
        addApiKey(newKey)
        setSelectedApiKey(newKey.id)
    }
    
    /**
     * 添加模型
     */
    fun addModel(model: DashScopeModel) {
        val currentModels = _models.toMutableList()
        currentModels.add(model)
        _models = currentModels
        saveModels(currentModels)
    }
    
    /**
     * 删除模型
     */
    fun removeModel(modelId: String) {
        val currentModels = _models.toMutableList()
        currentModels.removeAll { it.id == modelId }
        _models = currentModels
        saveModels(currentModels)
        
        // 如果删除的是当前选中的模型，清空选择
        if (_selectedModelId == modelId) {
            setSelectedModel("")
        }
    }
    
    /**
     * 更新模型
     */
    fun updateModel(updatedModel: DashScopeModel) {
        val currentModels = _models.toMutableList()
        val index = currentModels.indexOfFirst { it.id == updatedModel.id }
        if (index != -1) {
            currentModels[index] = updatedModel
            _models = currentModels
            saveModels(currentModels)
        }
    }
    
    /**
     * 设置选中的模型
     */
    fun setSelectedModel(modelId: String) {
        _selectedModelId = modelId
        prefs.edit().putString("selected_model_id", modelId).apply()

        // 同时更新该模型所属分组的偏好
        val model = _models.find { it.id == modelId }
        if (model != null) {
            updateSelectedModelForGroup(model.group, modelId)
        }
    }

    /**
     * 更新指定分组的选中模型
     */
    private fun updateSelectedModelForGroup(group: ModelGroup, modelId: String) {
        when (group) {
            ModelGroup.TEXT -> _selectedTextModelId = modelId
            ModelGroup.IMAGE -> _selectedImageModelId = modelId
            ModelGroup.VIDEO -> _selectedVideoModelId = modelId
        }
        prefs.edit().putString("selected_model_id_${group.name}", modelId).apply()
    }

    /**
     * 切换到指定模型分组，自动恢复该分组上次选中的模型
     */
    fun switchToModelGroup(group: ModelGroup) {
        val lastSelectedId = when (group) {
            ModelGroup.TEXT -> _selectedTextModelId
            ModelGroup.IMAGE -> _selectedImageModelId
            ModelGroup.VIDEO -> _selectedVideoModelId
        }

        // 如果该分组有上次选中的模型，则恢复；否则尝试选择该分组下的第一个模型
        if (lastSelectedId.isNotEmpty() && _models.any { it.id == lastSelectedId }) {
            setSelectedModel(lastSelectedId)
        } else {
            val firstModelInGroup = _models.find { it.group == group }
            if (firstModelInGroup != null) {
                setSelectedModel(firstModelInGroup.id)
            }
        }
    }
    
    /**
     * 获取选中的模型
     */
    fun getSelectedModel(): DashScopeModel? {
        return _models.find { it.id == _selectedModelId }
    }
    
    /**
     * 保存模型列表
     */
    private fun saveModels(modelsList: List<DashScopeModel>) {
        val modelsJson = gson.toJson(modelsList)
        prefs.edit().putString("models", modelsJson).apply()
    }
}

/**
 * 全局单例配置管理器
 */
object GlobalDashScopeConfigManager {
    @Volatile
    private var INSTANCE: DashScopeConfigManager? = null
    
    fun getInstance(context: Context): DashScopeConfigManager {
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: DashScopeConfigManager(context.applicationContext).also { INSTANCE = it }
        }
    }
}

/**
 * Composable函数用于记住DashScopeConfigManager实例
 * 使用全局单例确保不同页面间状态同步
 */
@Composable
fun rememberDashScopeConfigManager(): DashScopeConfigManager {
    val context = LocalContext.current
    return remember { GlobalDashScopeConfigManager.getInstance(context) }
}