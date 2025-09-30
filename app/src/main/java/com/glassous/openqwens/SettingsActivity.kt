package com.glassous.openqwens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.glassous.openqwens.ui.theme.DashScopeConfigManager
import com.glassous.openqwens.ui.theme.DashScopeModel
import com.glassous.openqwens.ui.theme.OpenQwensTheme
import com.glassous.openqwens.ui.theme.ThemeManager
import com.glassous.openqwens.ui.theme.ThemeMode
import com.glassous.openqwens.ui.theme.rememberDashScopeConfigManager
import com.glassous.openqwens.ui.theme.rememberThemeManager
import java.util.UUID

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeManager = rememberThemeManager()
            val dashScopeConfigManager = rememberDashScopeConfigManager()
            OpenQwensTheme(
                themeMode = themeManager.getThemeModeEnum()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SettingsScreen(
                        themeManager = themeManager,
                        dashScopeConfigManager = dashScopeConfigManager,
                        onBackClick = { 
                            finish()
                            overridePendingTransition(
                                com.glassous.openqwens.R.anim.slide_in_left,
                                com.glassous.openqwens.R.anim.slide_out_right
                            )
                        }
                    )
                }
            }
        }
    }
    
    // 处理系统返回键
    override fun onBackPressed() {
        super.onBackPressed()
        // 添加返回时的滑出动画
        overridePendingTransition(
            R.anim.slide_in_left,
            R.anim.slide_out_right
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeManager: ThemeManager,
    dashScopeConfigManager: DashScopeConfigManager,
    onBackClick: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "设置",
                        fontWeight = FontWeight.Medium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 主题模式设置
            ThemeModeSection(themeManager = themeManager)
            
            // 阿里云百炼模型配置
            DashScopeConfigSection(dashScopeConfigManager = dashScopeConfigManager)
        }
    }
}

@Composable
private fun DashScopeConfigSection(dashScopeConfigManager: DashScopeConfigManager) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 标题区域
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "阿里云百炼模型配置",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // 基础URL配置
            BaseUrlConfigSection(dashScopeConfigManager = dashScopeConfigManager)
            
            // API密钥配置
            ApiKeyConfigSection(dashScopeConfigManager = dashScopeConfigManager)
            
            // 模型管理
            ModelManagementSection(dashScopeConfigManager = dashScopeConfigManager)
        }
    }
}

@Composable
private fun BaseUrlConfigSection(dashScopeConfigManager: DashScopeConfigManager) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "基础URL",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        OutlinedTextField(
            value = dashScopeConfigManager.baseUrl,
            onValueChange = { dashScopeConfigManager.setBaseUrl(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { 
                Text(
                    text = "请输入基础URL",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ) 
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

@Composable
private fun ApiKeyConfigSection(dashScopeConfigManager: DashScopeConfigManager) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "API密钥",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        OutlinedTextField(
            value = dashScopeConfigManager.apiKey,
            onValueChange = { dashScopeConfigManager.setApiKey(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { 
                Text(
                    text = "请输入API密钥",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ) 
            },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            trailingIcon = null
        )
    }
}

@Composable
private fun ModelManagementSection(dashScopeConfigManager: DashScopeConfigManager) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingModel by remember { mutableStateOf<DashScopeModel?>(null) }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 标题和添加按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "模型管理",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            FilledTonalButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.height(40.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "添加",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
        
        // 模型列表
        if (dashScopeConfigManager.models.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "暂无模型",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "点击右上角添加按钮添加模型",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                dashScopeConfigManager.models.forEach { model ->
                    ModelItem(
                        model = model,
                        isSelected = model.id == dashScopeConfigManager.selectedModelId,
                        onSelect = { dashScopeConfigManager.setSelectedModel(model.id) },
                        onEdit = {
                            editingModel = model
                            showEditDialog = true
                        },
                        onDelete = { dashScopeConfigManager.removeModel(model.id) }
                    )
                }
            }
        }
    }
    
    // 添加模型对话框
    if (showAddDialog) {
        AddModelDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { model ->
                dashScopeConfigManager.addModel(model)
                showAddDialog = false
            }
        )
    }
    
    // 编辑模型对话框
    if (showEditDialog && editingModel != null) {
        EditModelDialog(
            model = editingModel!!,
            onDismiss = { 
                showEditDialog = false
                editingModel = null
            },
            onConfirm = { updatedModel ->
                dashScopeConfigManager.updateModel(updatedModel)
                showEditDialog = false
                editingModel = null
            }
        )
    }
}

@Composable
private fun ModelItem(
    model: DashScopeModel,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceContainer
        ),
        border = if (isSelected) 
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else 
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = model.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = if (isSelected) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onSurface
                    )
                }
                
                if (model.description.isNotEmpty()) {
                    Text(
                        text = model.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "编辑模型",
                        tint = if (isSelected) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "删除模型",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddModelDialog(
    onDismiss: () -> Unit,
    onConfirm: (DashScopeModel) -> Unit
) {
    var modelName by remember { mutableStateOf("") }
    var modelDescription by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加模型") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = modelName,
                    onValueChange = { modelName = it },
                    label = { Text("模型名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = modelDescription,
                    onValueChange = { modelDescription = it },
                    label = { Text("模型描述（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (modelName.isNotBlank()) {
                        onConfirm(
                            DashScopeModel(
                                id = modelName.trim(),
                                name = modelName.trim(),
                                description = modelDescription.trim()
                            )
                        )
                    }
                },
                enabled = modelName.isNotBlank()
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun EditModelDialog(
    model: DashScopeModel,
    onDismiss: () -> Unit,
    onConfirm: (DashScopeModel) -> Unit
) {
    var modelName by remember { mutableStateOf(model.name) }
    var modelDescription by remember { mutableStateOf(model.description) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑模型") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = modelName,
                    onValueChange = { modelName = it },
                    label = { Text("模型名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = modelDescription,
                    onValueChange = { modelDescription = it },
                    label = { Text("模型描述（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (modelName.isNotBlank()) {
                        onConfirm(
                            model.copy(
                                name = modelName.trim(),
                                description = modelDescription.trim()
                            )
                        )
                    }
                },
                enabled = modelName.isNotBlank()
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun ThemeModeSection(themeManager: ThemeManager) {
    val currentThemeMode = themeManager.getThemeMode()
    val themeOptions = listOf("跟随系统", "浅色", "深色")
    val selectedIndex = when (currentThemeMode) {
        ThemeMode.SYSTEM.value -> 0
        ThemeMode.LIGHT.value -> 1
        ThemeMode.DARK.value -> 2
        else -> 0
    }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "主题模式",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            themeOptions.forEachIndexed { index, option ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = themeOptions.size
                    ),
                    onClick = {
                        val newThemeMode = when (index) {
                            0 -> ThemeMode.SYSTEM
                            1 -> ThemeMode.LIGHT
                            2 -> ThemeMode.DARK
                            else -> ThemeMode.SYSTEM
                        }
                        themeManager.setThemeMode(newThemeMode)
                    },
                    selected = index == selectedIndex
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}