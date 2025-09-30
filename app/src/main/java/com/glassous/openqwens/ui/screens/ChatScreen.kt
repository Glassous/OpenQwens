package com.glassous.openqwens.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.glassous.openqwens.data.ChatSession
import com.glassous.openqwens.ui.components.ChatInputBar
import com.glassous.openqwens.ui.components.ChatMessageItem
import com.glassous.openqwens.ui.components.ChatSessionItem
import com.glassous.openqwens.ui.components.DeleteSessionDialog
import com.glassous.openqwens.ui.components.RenameSessionDialog
import com.glassous.openqwens.ui.components.AttachmentBottomSheet
import com.glassous.openqwens.ui.components.FunctionCardList
import com.glassous.openqwens.ui.components.SelectedFunction
import com.glassous.openqwens.ui.components.FunctionType
import com.glassous.openqwens.ui.components.AttachmentCardList
import com.glassous.openqwens.ui.components.AttachmentData
import com.glassous.openqwens.ui.components.MixedCardList
import com.glassous.openqwens.ui.components.FunctionExclusionManager
import com.glassous.openqwens.utils.FileUtils
import com.glassous.openqwens.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import android.content.Intent
import com.glassous.openqwens.SettingsActivity
import com.glassous.openqwens.ui.theme.DashScopeConfigManager
import com.glassous.openqwens.ui.theme.rememberDashScopeConfigManager
import java.util.Calendar
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.runtime.remember
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.Manifest
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.glassous.openqwens.utils.ImageUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel()
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 获取DashScopeConfigManager实例
    val dashScopeConfigManager = rememberDashScopeConfigManager()
    
    // 获取时间段问候语
    val greetingInfo = remember {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> Pair("上午好", "新的一天开始了，我们一起探索知识的海洋")
            in 12..13 -> Pair("中午好", "午间时光，有什么问题想要了解的吗？")
            in 14..17 -> Pair("下午好", "下午时光正好，让我来帮助您解决问题")
            in 18..22 -> Pair("晚上好", "夜幕降临，我在这里为您答疑解惑")
            else -> Pair("夜深了", "深夜时分，我依然在这里陪伴您")
        }
    }
    
    val currentSession by viewModel.currentSession.collectAsState()
    val chatSessions by viewModel.chatSessions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val streamingContent by viewModel.streamingContent.collectAsState()
    
    val listState = rememberLazyListState()
    
    // Bottom Sheet 状态
    var showAttachmentBottomSheet by remember { mutableStateOf(false) }
    
    // 选中的功能列表状态
    var selectedFunctions by remember { mutableStateOf(listOf<SelectedFunction>()) }
    
    // 选中的附件列表状态
    var selectedAttachments by remember { mutableStateOf(listOf<AttachmentData>()) }
    
    // 文件选择器
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val attachment = FileUtils.createAttachmentFromUri(context, it)
            attachment?.let { attachmentData ->
                // 检查文件大小
                if (FileUtils.isFileSizeValid(attachmentData.fileSize)) {
                    // 检查是否已经选择了该文件，避免重复添加
                    if (!selectedAttachments.any { it.fileName == attachmentData.fileName }) {
                        selectedAttachments = selectedAttachments + attachmentData
                    }
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar("文件大小超过限制（最大10MB）")
                    }
                }
            } ?: run {
                scope.launch {
                    snackbarHostState.showSnackbar("文件读取失败")
                }
            }
        }
    }
    
    // 创建临时图片文件
    val cameraImageUri = remember {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFile = File(context.cacheDir, "camera_image_$timeStamp.jpg")
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    }
    
    // 相机启动器
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            // 拍照成功，处理图片
            val attachment = FileUtils.createAttachmentFromUri(context, cameraImageUri)
            attachment?.let { attachmentData ->
                // 检查文件大小
                if (FileUtils.isFileSizeValid(attachmentData.fileSize)) {
                    selectedAttachments = selectedAttachments + attachmentData
                    scope.launch {
                        snackbarHostState.showSnackbar("照片已添加")
                    }
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar("照片文件过大")
                    }
                }
            } ?: run {
                scope.launch {
                    snackbarHostState.showSnackbar("照片处理失败")
                }
            }
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("拍照已取消")
            }
        }
    }
    
    // 相机权限请求
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 权限已授予，启动相机
            cameraLauncher.launch(cameraImageUri)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("需要相机权限才能拍照")
            }
        }
    }
    
    // 处理返回键：如果侧边栏打开，则关闭侧边栏而不是退出应用
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch {
            drawerState.close()
        }
    }
    
    // 自动滚动到最新消息
    LaunchedEffect(currentSession?.messages?.size) {
        currentSession?.messages?.let { messages ->
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }
    
    // 处理功能选择
    val handleFunctionSelected: (FunctionType) -> Unit = { functionType ->
        // 如果是附件类型，启动文件选择器
        if (functionType in listOf(FunctionType.IMAGE, FunctionType.VIDEO, FunctionType.AUDIO, FunctionType.FILE)) {
            when (functionType) {
                FunctionType.IMAGE -> filePickerLauncher.launch("image/*")
                FunctionType.VIDEO -> filePickerLauncher.launch("video/*")
                FunctionType.AUDIO -> filePickerLauncher.launch("audio/*")
                FunctionType.FILE -> filePickerLauncher.launch("*/*")
                else -> {}
            }
        } else if (functionType == FunctionType.CAMERA) {
            // 相机功能，请求权限并启动相机
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            // 功能类型，添加到功能列表
            val newFunction = SelectedFunction(
                id = functionType.id,
                name = functionType.displayName,
                description = functionType.description,
                icon = when (functionType) {
                    FunctionType.DEEP_THINKING -> Icons.Default.Psychology
                    FunctionType.WEB_SEARCH -> Icons.Default.Search
                    FunctionType.IMAGE_GENERATION -> Icons.Default.Palette
                    FunctionType.IMAGE_EDITING -> Icons.Default.Edit
                    FunctionType.VIDEO_GENERATION -> Icons.Default.Movie
                    FunctionType.CAMERA -> Icons.Default.CameraAlt
                    else -> Icons.Default.Functions
                }
            )
            
            // 检查是否已经选择了该功能，避免重复添加
            if (!selectedFunctions.any { it.id == newFunction.id }) {
                selectedFunctions = selectedFunctions + newFunction
            }
        }
    }
    
    // 处理功能移除
    val handleFunctionRemoved: (SelectedFunction) -> Unit = { function ->
        selectedFunctions = selectedFunctions.filter { it.id != function.id }
    }
    
    // 处理附件移除
    val handleAttachmentRemoved: (AttachmentData) -> Unit = { attachment ->
        selectedAttachments = selectedAttachments.filter { it.id != attachment.id }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            NavigationDrawerContent(
                chatSessions = chatSessions,
                currentSession = currentSession,
                onNewChat = { 
                    viewModel.createNewChat()
                    scope.launch { drawerState.close() }
                },
                onSelectSession = { session ->
                    viewModel.selectSession(session)
                    scope.launch { drawerState.close() }
                },
                onDeleteSession = { sessionId ->
                    viewModel.deleteSession(sessionId)
                },
                onRenameSession = { sessionId, newTitle ->
                    viewModel.renameSession(sessionId, newTitle)
                }
            )
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        // 根据模型选择状态显示不同的标题，使用状态监听实现实时更新
                        val selectedModelId by remember { derivedStateOf { dashScopeConfigManager.selectedModelId } }
                        val models by remember { derivedStateOf { dashScopeConfigManager.models } }
                        val selectedModel = models.find { it.id == selectedModelId }
                        val titleText = when {
                            selectedModel != null -> selectedModel.name
                            else -> "OpenQwens"
                        }
                        Text(
                            text = titleText,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    if (drawerState.isClosed) {
                                        drawerState.open()
                                    } else {
                                        drawerState.close()
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "菜单"
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                val intent = Intent(context, SettingsActivity::class.java)
                                context.startActivity(intent)
                                // 添加从右侧滑入的动画
                                (context as? android.app.Activity)?.overridePendingTransition(
                                    com.glassous.openqwens.R.anim.slide_in_right,
                                    com.glassous.openqwens.R.anim.slide_out_left
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "设置"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .background(Color.Transparent) // 使用Color.Transparent替代androidx.compose.ui.graphics.Color.Transparent
                        .fillMaxWidth()
                ) {
                    // 混合卡片列表（附件和功能卡片在同一行）
                    MixedCardList(
                        selectedAttachments = selectedAttachments,
                        selectedFunctions = selectedFunctions,
                        onRemoveAttachment = handleAttachmentRemoved,
                        onRemoveFunction = handleFunctionRemoved,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .background(Color.Transparent) // 强制透明背景
                    )
                    
                    // 输入框
                    ChatInputBar(
                        onSendMessage = { message ->
                            viewModel.sendMessageStream(message, selectedFunctions)
                        },
                        onShowAttachmentOptions = { showAttachmentBottomSheet = true }, // 显示附件选项
                        isLoading = isLoading || isStreaming // 流式输出时也显示为加载状态
                    )
                }
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (currentSession?.messages?.isEmpty() == true) {
                    // 空状态 - 显示时间段问候语
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = greetingInfo.first,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = greetingInfo.second,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // 聊天消息列表
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        currentSession?.messages?.let { messages ->
                            items(messages) { message ->
                                ChatMessageItem(
                                    message = message,
                                    onShowSnackbar = { text ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar(text)
                                        }
                                    }
                                )
                            }
                        }
                        
                        // 显示加载动画（当正在等待API响应时）
                        if (isLoading) {
                            item {
                                LoadingIndicatorItem()
                            }
                        }
                    }
                }
            }
        }
    }
    
    // 显示附件选项 Bottom Sheet
    if (showAttachmentBottomSheet) {
        AttachmentBottomSheet(
            onDismiss = { showAttachmentBottomSheet = false },
            onFunctionSelected = handleFunctionSelected,
            selectedFunctions = selectedFunctions,
            selectedAttachments = selectedAttachments
        )
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationDrawerContent(
    chatSessions: List<ChatSession>,
    currentSession: ChatSession?,
    onNewChat: () -> Unit,
    onSelectSession: (ChatSession) -> Unit,
    onDeleteSession: (String) -> Unit,
    onRenameSession: (String, String) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf<ChatSession?>(null) }
    var showRenameDialog by remember { mutableStateOf<ChatSession?>(null) }
    val context = LocalContext.current
    
    // 获取所有生成的图片
    val generatedImages = remember { ImageUtils.getAIGeneratedImages(context) }
    
    ModalDrawerSheet {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(16.dp)
        ) {
            // 标题
            Text(
                text = "OpenQwens",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            
            HorizontalDivider()
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // MD3 Carousel 组件 - 显示生成的图片
            if (generatedImages.isNotEmpty()) {
                Text(
                    text = "生成的图片",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                val carouselState = rememberCarouselState { generatedImages.size }
                
                HorizontalMultiBrowseCarousel(
                    state = carouselState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .padding(bottom = 16.dp),
                    preferredItemWidth = 100.dp,
                    itemSpacing = 8.dp
                ) { index ->
                    val imagePath = generatedImages[index]
                    
                    Card(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                // 点击图片时可以添加预览功能
                            },
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imagePath)
                                .crossfade(true)
                                .build(),
                            contentDescription = "生成的图片 ${index + 1}",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // 新建聊天按钮
            NavigationDrawerItem(
                label = { 
                    Text(
                        text = "新建聊天",
                        fontWeight = FontWeight.Medium
                    ) 
                },
                selected = false,
                onClick = onNewChat,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "新建聊天"
                    )
                },
                modifier = Modifier.padding(vertical = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 聊天记录标题
            Text(
                text = "聊天记录",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // 聊天会话列表
            LazyColumn {
                items(chatSessions) { session ->
                    ChatSessionItem(
                        session = session,
                        isSelected = currentSession?.id == session.id,
                        onClick = { onSelectSession(session) },
                        onDelete = { showDeleteDialog = session },
                        onRename = { showRenameDialog = session }
                    )
                }
            }
        }
    }
    
    // 删除对话框
    showDeleteDialog?.let { session ->
        DeleteSessionDialog(
            sessionTitle = session.title,
            onConfirm = { onDeleteSession(session.id) },
            onDismiss = { showDeleteDialog = null }
        )
    }
    
    // 重命名对话框
    showRenameDialog?.let { session ->
        RenameSessionDialog(
            currentTitle = session.title,
            onConfirm = { newTitle -> onRenameSession(session.id, newTitle) },
            onDismiss = { showRenameDialog = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoadingIndicatorItem() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = 4.dp,
                bottomEnd = 16.dp
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "正在思考...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}