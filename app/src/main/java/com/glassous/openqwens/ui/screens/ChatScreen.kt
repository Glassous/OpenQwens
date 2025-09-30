package com.glassous.openqwens.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.glassous.openqwens.data.ChatSession
import com.glassous.openqwens.ui.components.ChatInputBar
import com.glassous.openqwens.ui.components.ChatMessageItem
import com.glassous.openqwens.ui.components.ChatSessionItem
import com.glassous.openqwens.ui.components.DeleteSessionDialog
import com.glassous.openqwens.ui.components.RenameSessionDialog
import com.glassous.openqwens.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import android.content.Intent
import com.glassous.openqwens.SettingsActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel()
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    val currentSession by viewModel.currentSession.collectAsState()
    val chatSessions by viewModel.chatSessions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    val listState = rememberLazyListState()
    
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
                        Text(
                            text = currentSession?.title ?: "OpenQwens",
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
                ChatInputBar(
                    onSendMessage = viewModel::sendMessage,
                    isLoading = isLoading
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (currentSession?.messages?.isEmpty() == true) {
                    // 空状态
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "👋",
                            style = MaterialTheme.typography.displayLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "欢迎使用 OpenQwens",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "开始对话吧！",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                ChatMessageItem(message = message)
                            }
                        }
                    }
                }
            }
        }
    }
}

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