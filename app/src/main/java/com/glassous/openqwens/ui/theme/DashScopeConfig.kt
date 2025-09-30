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
 * 阿里云百炼模型配置数据类
 */
data class DashScopeModel(
    val id: String,
    val name: String,
    val description: String = ""
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
    private var _apiKey by mutableStateOf(loadApiKey())
    private var _models by mutableStateOf(loadModels())
    private var _selectedModelId by mutableStateOf(loadSelectedModelId())
    
    val baseUrl: String get() = _baseUrl
    val apiKey: String get() = _apiKey
    val models: List<DashScopeModel> get() = _models
    val selectedModelId: String get() = _selectedModelId
    
    /**
     * 加载基础URL
     */
    private fun loadBaseUrl(): String {
        return prefs.getString("base_url", defaultBaseUrl) ?: defaultBaseUrl
    }
    
    /**
     * 加载API密钥
     */
    private fun loadApiKey(): String {
        return prefs.getString("api_key", "") ?: ""
    }
    
    /**
     * 加载选中的模型ID
     */
    private fun loadSelectedModelId(): String {
        return prefs.getString("selected_model_id", "") ?: ""
    }
    
    /**
     * 加载所有模型
     */
    private fun loadModels(): List<DashScopeModel> {
        val modelsJson = prefs.getString("models", "[]") ?: "[]"
        val type = object : TypeToken<List<DashScopeModel>>() {}.type
        return try {
            gson.fromJson(modelsJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 设置基础URL
     */
    fun setBaseUrl(url: String) {
        _baseUrl = url
        prefs.edit().putString("base_url", url).apply()
    }
    
    /**
     * 设置API密钥
     */
    fun setApiKey(key: String) {
        _apiKey = key
        prefs.edit().putString("api_key", key).apply()
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