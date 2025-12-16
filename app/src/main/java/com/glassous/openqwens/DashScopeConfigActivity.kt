package com.glassous.openqwens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.mutableStateMapOf
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
import com.glassous.openqwens.ui.theme.rememberDashScopeConfigManager
import com.glassous.openqwens.ui.theme.rememberThemeManager

import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.automirrored.filled.HelpOutline

class DashScopeConfigActivity : ComponentActivity() {
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
                    DashScopeConfigScreen(
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

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(
            R.anim.slide_in_left,
            R.anim.slide_out_right
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashScopeConfigScreen(
    dashScopeConfigManager: DashScopeConfigManager,
    onBackClick: () -> Unit
) {
    var showTutorialDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("模型配置", fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showTutorialDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = "教程"
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
                .padding(horizontal = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DashScopeConfigSection(dashScopeConfigManager = dashScopeConfigManager)
        }
        
        if (showTutorialDialog) {
            TutorialDialog(onDismiss = { showTutorialDialog = false })
        }
    }
}

@Composable
@Suppress("DEPRECATION")
private fun TutorialDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("使用教程") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. 注册账号
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "1. 注册账号", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    val registerText = buildAnnotatedString {
                        append("如果没有阿里云账号，您需要先")
                        
                        pushStringAnnotation(tag = "URL", annotation = "https://account.aliyun.com/register/qr_register.htm")
                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                            append("注册阿里云账号")
                        }
                        pop()
                        
                        append("。")
                    }
                    ClickableText(
                        text = registerText,
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        onClick = { offset ->
                            registerText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                .firstOrNull()?.let { uriHandler.openUri(it.item) }
                        }
                    )
                }

                // 2. 开通阿里云百炼
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "2. 开通阿里云百炼", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    val serviceText = buildAnnotatedString {
                        append("使用阿里云主账号前往阿里云百炼大模型服务平台（")
                        
                        pushStringAnnotation(tag = "URL", annotation = "https://bailian.console.aliyun.com/?tab=model#/model-market")
                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                            append("北京")
                        }
                        pop()
                        
                        append("或")
                        
                        pushStringAnnotation(tag = "URL", annotation = "https://modelstudio.console.aliyun.com/?tab=model#/model-market")
                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                            append("新加坡")
                        }
                        pop()
                        
                        append("），阅读并同意协议后，将自动开通阿里云百炼，如果未弹出服务协议，则表示您已经开通。\n如果开通服务时提示“您尚未进行实名认证”，请先进行")
                        
                        pushStringAnnotation(tag = "URL", annotation = "https://help.aliyun.com/zh/account/verify-your-identity-individual-account")
                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                            append("实名认证")
                        }
                        pop()
                        
                        append("。")
                    }
                    ClickableText(
                        text = serviceText,
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        onClick = { offset ->
                            serviceText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                .firstOrNull()?.let { uriHandler.openUri(it.item) }
                        }
                    )
                }

                // 3. 获取API Key
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "3. 获取API Key", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    val keyText = buildAnnotatedString {
                        append("前往密钥管理（")
                        
                        pushStringAnnotation(tag = "URL", annotation = "https://bailian.console.aliyun.com/?tab=model#/api-key")
                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                            append("北京")
                        }
                        pop()
                        
                        append("或")
                        
                        pushStringAnnotation(tag = "URL", annotation = "https://modelstudio.console.aliyun.com/?tab=model#/api-key")
                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                            append("新加坡")
                        }
                        pop()
                        
                        append("）页面，单击创建API-KEY，即可通过API KEY调用大模型。\n创建新的API Key时，归属业务空间推荐选择主账号空间。使用子空间API Key需由主账号管理员为对应子空间开通模型授权（如本文使用通义千问-Plus模型），详情请参见")
                        
                        pushStringAnnotation(tag = "URL", annotation = "https://help.aliyun.com/zh/model-studio/use-workspace#f2e68d7ba7ubk")
                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                            append("授权子业务空间模型调用、训练和部署")
                        }
                        pop()
                        
                        append("。")
                    }
                    ClickableText(
                        text = keyText,
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        onClick = { offset ->
                            keyText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                .firstOrNull()?.let { uriHandler.openUri(it.item) }
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
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
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 删除重复的标题，因为顶部栏已有
            BaseUrlConfigSection(dashScopeConfigManager = dashScopeConfigManager)
            ApiKeyConfigSection(dashScopeConfigManager = dashScopeConfigManager)
            ModelManagementSection(dashScopeConfigManager = dashScopeConfigManager)
        }
    }
}

@Composable
private fun BaseUrlConfigSection(dashScopeConfigManager: DashScopeConfigManager) {
    Column(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Base URL",
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
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingApiKey by remember { mutableStateOf<com.glassous.openqwens.ui.theme.ApiKey?>(null) }

    Column(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "API Key",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            FilledTonalButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "添加",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        if (dashScopeConfigManager.apiKeys.isEmpty()) {
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
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "暂无API密钥",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "点击上方添加按钮添加API密钥",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                dashScopeConfigManager.apiKeys.forEach { apiKey ->
                    ApiKeyItem(
                        apiKey = apiKey,
                        isSelected = apiKey.id == dashScopeConfigManager.selectedApiKeyId,
                        onSelect = { dashScopeConfigManager.setSelectedApiKey(apiKey.id) },
                        onEdit = {
                            editingApiKey = apiKey
                            showEditDialog = true
                        },
                        onDelete = { dashScopeConfigManager.removeApiKey(apiKey.id) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddApiKeyDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, key ->
                dashScopeConfigManager.addApiKey(
                    com.glassous.openqwens.ui.theme.ApiKey(
                        name = name,
                        key = key
                    )
                )
                showAddDialog = false
            }
        )
    }

    if (showEditDialog && editingApiKey != null) {
        EditApiKeyDialog(
            apiKey = editingApiKey!!,
            onDismiss = {
                showEditDialog = false
                editingApiKey = null
            },
            onConfirm = { updatedKey ->
                dashScopeConfigManager.updateApiKey(updatedKey)
                showEditDialog = false
                editingApiKey = null
            }
        )
    }
}

@Composable
private fun ApiKeyItem(
    apiKey: com.glassous.openqwens.ui.theme.ApiKey,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                .padding(12.dp),
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

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = apiKey.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (apiKey.key.length > 8) "${apiKey.key.take(4)}...${apiKey.key.takeLast(4)}" else apiKey.key,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "编辑",
                        modifier = Modifier.size(16.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "删除",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun AddApiKeyDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加API密钥") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("API Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, key) },
                enabled = name.isNotBlank() && key.isNotBlank()
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun EditApiKeyDialog(
    apiKey: com.glassous.openqwens.ui.theme.ApiKey,
    onDismiss: () -> Unit,
    onConfirm: (com.glassous.openqwens.ui.theme.ApiKey) -> Unit
) {
    var name by remember { mutableStateOf(apiKey.name) }
    var key by remember { mutableStateOf(apiKey.key) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑API密钥") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("API Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(apiKey.copy(name = name, key = key)) },
                enabled = name.isNotBlank() && key.isNotBlank()
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun ModelManagementSection(dashScopeConfigManager: DashScopeConfigManager) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingModel by remember { mutableStateOf<DashScopeModel?>(null) }
    
    val expandedGroups = remember {
        mutableStateMapOf<com.glassous.openqwens.ui.theme.ModelGroup, Boolean>().apply {
            com.glassous.openqwens.ui.theme.ModelGroup.values().forEach { put(it, true) }
        }
    }

    Column(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                com.glassous.openqwens.ui.theme.ModelGroup.values().forEach { group ->
                    val groupModels = dashScopeConfigManager.models.filter { it.group == group }
                    if (groupModels.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        expandedGroups[group] = !(expandedGroups[group] ?: true)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = group.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = if (expandedGroups[group] == true) 
                                        Icons.Filled.KeyboardArrowUp 
                                    else 
                                        Icons.Filled.KeyboardArrowDown,
                                    contentDescription = if (expandedGroups[group] == true) "收起" else "展开",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            AnimatedVisibility(
                                visible = expandedGroups[group] == true,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    groupModels.forEach { model ->
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
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddModelDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { model ->
                dashScopeConfigManager.addModel(model)
                showAddDialog = false
            }
        )
    }

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
        modifier = Modifier.fillMaxWidth(),
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
                .padding(12.dp),
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
                    
                    Surface(
                        color = if (isSelected) 
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) 
                        else 
                            MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = model.group.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                if (model.description.isNotEmpty()) {
                    Text(
                        text = model.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (model.note.isNotEmpty()) {
                    Text(
                        text = "备注: ${model.note}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
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
    var modelNote by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf(com.glassous.openqwens.ui.theme.ModelGroup.TEXT) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加模型") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
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
                
                OutlinedTextField(
                    value = modelNote,
                    onValueChange = { modelNote = it },
                    label = { Text("备注（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("模型分组", style = MaterialTheme.typography.bodyMedium)
                    com.glassous.openqwens.ui.theme.ModelGroup.values().forEach { group ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedGroup = group }
                        ) {
                            RadioButton(
                                selected = (selectedGroup == group),
                                onClick = { selectedGroup = group }
                            )
                            Text(
                                text = group.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
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
                                description = modelDescription.trim(),
                                group = selectedGroup,
                                note = modelNote.trim()
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
    var modelNote by remember { mutableStateOf(model.note) }
    var selectedGroup by remember { mutableStateOf(model.group) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑模型") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
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
                
                OutlinedTextField(
                    value = modelNote,
                    onValueChange = { modelNote = it },
                    label = { Text("备注（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("模型分组", style = MaterialTheme.typography.bodyMedium)
                    com.glassous.openqwens.ui.theme.ModelGroup.values().forEach { group ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedGroup = group }
                        ) {
                            RadioButton(
                                selected = (selectedGroup == group),
                                onClick = { selectedGroup = group }
                            )
                            Text(
                                text = group.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (modelName.isNotBlank()) {
                        onConfirm(
                            model.copy(
                                name = modelName.trim(),
                                description = modelDescription.trim(),
                                group = selectedGroup,
                                note = modelNote.trim()
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