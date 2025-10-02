package com.glassous.openqwens.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// 菜单类型枚举
enum class MenuType {
    MAIN,           // 主菜单
    ATTACHMENT,     // 附件类型菜单
    FUNCTION        // 功能菜单
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentBottomSheet(
    onDismiss: () -> Unit,
    onFunctionSelected: (FunctionType) -> Unit = {},
    selectedFunctions: List<SelectedFunction> = emptyList(),
    selectedAttachments: List<AttachmentData> = emptyList(),
    modifier: Modifier = Modifier
) {
    var currentMenu by remember { mutableStateOf(MenuType.MAIN) }
    
    // 处理返回键，在二级菜单时返回到主菜单
    BackHandler(enabled = currentMenu != MenuType.MAIN) {
        currentMenu = MenuType.MAIN
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        // 使用AnimatedContent实现菜单切换动画
        AnimatedContent(
            targetState = currentMenu,
            transitionSpec = {
                // 根据菜单切换方向选择动画
                if (targetState == MenuType.MAIN) {
                    // 返回主菜单：从右向左滑入
                    slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    ) + fadeIn(
                        animationSpec = tween(300)
                    ) togetherWith slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    ) + fadeOut(
                        animationSpec = tween(300)
                    )
                } else {
                    // 进入子菜单：从左向右滑入
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    ) + fadeIn(
                        animationSpec = tween(300)
                    ) togetherWith slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    ) + fadeOut(
                        animationSpec = tween(300)
                    )
                }
            },
            label = "menu_transition"
        ) { menuType ->
            when (menuType) {
                MenuType.MAIN -> MainMenuContent(
                    onAttachmentClick = { currentMenu = MenuType.ATTACHMENT },
                    onFunctionClick = { currentMenu = MenuType.FUNCTION }
                )
                MenuType.ATTACHMENT -> AttachmentMenuContent(
                    onBackClick = { currentMenu = MenuType.MAIN },
                    onAttachmentSelected = { functionType ->
                        onFunctionSelected(functionType)
                        onDismiss()
                    },
                    selectedFunctions = selectedFunctions,
                    selectedAttachments = selectedAttachments
                )
                MenuType.FUNCTION -> FunctionMenuContent(
                    onBackClick = { currentMenu = MenuType.MAIN },
                    onFunctionSelected = { functionType ->
                        onFunctionSelected(functionType)
                        onDismiss()
                    },
                    selectedFunctions = selectedFunctions,
                    selectedAttachments = selectedAttachments
                )
            }
        }
    }
}

@Composable
fun MainMenuContent(
    onAttachmentClick: () -> Unit,
    onFunctionClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // 标题
        Text(
            text = "选择操作类型",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // 主菜单选项
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MainMenuOptionRow(
                icon = Icons.Default.Attachment,
                title = "选择附件类型",
                description = "上传文件、图片、视频等",
                onClick = onAttachmentClick
            )
            
            MainMenuOptionRow(
                icon = Icons.Default.Functions,
                title = "选择功能",
                description = "深度思考、联网搜索、图片生成等",
                onClick = onFunctionClick
            )
        }
        
        // 底部间距
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun AttachmentMenuContent(
    onBackClick: () -> Unit,
    onAttachmentSelected: (FunctionType) -> Unit = {},
    selectedFunctions: List<SelectedFunction> = emptyList(),
    selectedAttachments: List<AttachmentData> = emptyList()
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // 标题栏带返回按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回"
                )
            }
            
            Text(
                text = "选择附件类型",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        
        // 附件选项
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AttachmentOptionRow(
                icon = Icons.Default.CameraAlt,
                title = "相机",
                description = "拍照或录像",
                onClick = { onAttachmentSelected(FunctionType.CAMERA) },
                enabled = !FunctionExclusionManager.shouldBeDisabled(
                    FunctionType.CAMERA, selectedFunctions, selectedAttachments
                )
            )
            
            AttachmentOptionRow(
                icon = Icons.Default.Image,
                title = "图片",
                description = "从相册选择",
                onClick = { onAttachmentSelected(FunctionType.IMAGE) },
                enabled = !FunctionExclusionManager.shouldBeDisabled(
                    FunctionType.IMAGE, selectedFunctions, selectedAttachments
                )
            )
            

        }
        
        // 底部间距
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun FunctionMenuContent(
    onBackClick: () -> Unit,
    onFunctionSelected: (FunctionType) -> Unit = {},
    selectedFunctions: List<SelectedFunction> = emptyList(),
    selectedAttachments: List<AttachmentData> = emptyList()
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // 标题栏带返回按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回"
                )
            }
            
            Text(
                text = "选择功能",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        
        // 功能选项
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AttachmentOptionRow(
                icon = Icons.Default.Psychology,
                title = "深度思考",
                description = "启用深度推理模式",
                onClick = { onFunctionSelected(FunctionType.DEEP_THINKING) },
                enabled = !FunctionExclusionManager.shouldBeDisabled(
                    FunctionType.DEEP_THINKING, selectedFunctions, selectedAttachments
                )
            )
            
            AttachmentOptionRow(
                icon = Icons.Default.Search,
                title = "联网搜索",
                description = "搜索最新信息",
                onClick = { onFunctionSelected(FunctionType.WEB_SEARCH) },
                enabled = !FunctionExclusionManager.shouldBeDisabled(
                    FunctionType.WEB_SEARCH, selectedFunctions, selectedAttachments
                )
            )
            
            AttachmentOptionRow(
                icon = Icons.Default.Palette,
                title = "图片生成",
                description = "AI生成图片",
                onClick = { onFunctionSelected(FunctionType.IMAGE_GENERATION) },
                enabled = !FunctionExclusionManager.shouldBeDisabled(
                    FunctionType.IMAGE_GENERATION, selectedFunctions, selectedAttachments
                )
            )
            
            AttachmentOptionRow(
                icon = Icons.Default.Edit,
                title = "图片编辑",
                description = "编辑和修改图片",
                onClick = { onFunctionSelected(FunctionType.IMAGE_EDITING) },
                enabled = !FunctionExclusionManager.shouldBeDisabled(
                    FunctionType.IMAGE_EDITING, selectedFunctions, selectedAttachments
                )
            )
            

            
            // 视觉理解按钮已删除，因为上传图片时会自动触发视觉理解功能
        }
        
        // 底部间距
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuOptionRow(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            // 统一使用与二级菜单相同的颜色
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "进入",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentOptionRow(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Card(
        onClick = if (enabled) onClick else { {} },
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) 
                MaterialTheme.colorScheme.surfaceVariant 
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(24.dp),
                tint = if (enabled) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) 
                        MaterialTheme.colorScheme.onSurface 
                    else 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) 
                        MaterialTheme.colorScheme.onSurfaceVariant 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            }
        }
    }
}