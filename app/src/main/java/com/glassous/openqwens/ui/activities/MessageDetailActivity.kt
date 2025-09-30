package com.glassous.openqwens.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.glassous.openqwens.ui.theme.OpenQwensTheme
import com.glassous.openqwens.ui.theme.rememberThemeManager
import java.text.SimpleDateFormat
import java.util.*

class MessageDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val messageContent = intent.getStringExtra("message_content") ?: ""
        val messageTimestamp = intent.getLongExtra("message_timestamp", 0L)
        val isFromUser = intent.getBooleanExtra("message_is_from_user", false)

        setContent {
            val themeManager = rememberThemeManager()

            OpenQwensTheme(
                themeMode = themeManager.getThemeModeEnum()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MessageDetailScreen(
                        messageContent = messageContent,
                        messageTimestamp = messageTimestamp,
                        isFromUser = isFromUser,
                        onBackClick = {
                            finish()
                            // 添加返回动画
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageDetailScreen(
    messageContent: String,
    messageTimestamp: Long,
    isFromUser: Boolean,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 1. 创建一个状态变量来保存可编辑的文本内容
    //    初始值为传入的 messageContent
    var editableMessageContent by remember { mutableStateOf(messageContent) }

    val timeFormat = SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault())
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "消息详情",
                        style = MaterialTheme.typography.titleLarge,
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
                .padding(horizontal = 24.dp) // 只保留水平 padding，让输入框上下贴边
                .verticalScroll(scrollState)
        ) {
            // 2. 将 Text 组件替换为 TextField 组件
            TextField(
                value = editableMessageContent, // 绑定状态变量
                onValueChange = { newValue ->
                    editableMessageContent = newValue // 当文本改变时，更新状态变量
                },
                modifier = Modifier.fillMaxSize(), // 让输入框填满整个 Column
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    // 应用和之前 Text 组件一致的样式
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.4
                ),
                // 3. 自定义颜色，让它看起来像一个可编辑的文档页面
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent, // 移除焦点状态的下划线
                    unfocusedIndicatorColor = Color.Transparent, // 移除非焦点状态的下划线
                    cursorColor = MaterialTheme.colorScheme.primary // 可以自定义光标颜色
                )
            )
        }
    }
}