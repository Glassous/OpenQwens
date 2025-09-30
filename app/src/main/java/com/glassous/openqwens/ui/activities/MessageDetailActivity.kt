package com.glassous.openqwens.ui.activities

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.glassous.openqwens.ui.theme.OpenQwensTheme
import com.glassous.openqwens.ui.theme.rememberThemeManager
import com.glassous.openqwens.ui.components.ImagePreviewDialog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MessageDetailActivity : ComponentActivity() {
    
    // 图片加载器
    private val imageLoader by lazy { ImageLoader(this) }
    
    // 权限请求启动器
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "权限已授予", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "需要存储权限才能保存图片", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val messageContent = intent.getStringExtra("message_content") ?: ""
        val messageTimestamp = intent.getLongExtra("message_timestamp", 0L)
        val isFromUser = intent.getBooleanExtra("is_from_user", false)
        
        // 获取图片数据
        val imageUrls = intent.getStringArrayListExtra("image_urls") ?: arrayListOf()
        val localImagePaths = intent.getStringArrayListExtra("local_image_paths") ?: arrayListOf()

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
                        imageUrls = imageUrls,
                        localImagePaths = localImagePaths,
                        onBackClick = {
                            finish()
                            // 添加返回动画
                            overridePendingTransition(
                                com.glassous.openqwens.R.anim.slide_in_left,
                                com.glassous.openqwens.R.anim.slide_out_right
                            )
                        },
                        onSaveImage = { imagePath ->
                            saveImageToGallery(imagePath)
                        }
                    )
                }
            }
        }
    }
    
    private fun saveImageToGallery(imagePath: String) {
        try {
            // 检查权限
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            }
            
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(permission)
                return
            }
            
            // 判断是本地文件还是网络URL
            val bitmap = if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                // 网络图片，使用Coil加载
                lifecycleScope.launch {
                    try {
                        val imageRequest = ImageRequest.Builder(this@MessageDetailActivity)
                            .data(imagePath)
                            .build()
                        val drawable = imageLoader.execute(imageRequest).drawable
                        val bitmap = (drawable as? BitmapDrawable)?.bitmap
                        if (bitmap != null) {
                            saveBitmapToGallery(bitmap)
                        } else {
                            Toast.makeText(this@MessageDetailActivity, "无法加载网络图片", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(this@MessageDetailActivity, "下载图片失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                return
            } else {
                // 本地文件
                val file = java.io.File(imagePath)
                if (!file.exists()) {
                    Toast.makeText(this, "图片文件不存在", Toast.LENGTH_SHORT).show()
                    return
                }
                BitmapFactory.decodeFile(imagePath)
            }
            
            if (bitmap == null) {
                Toast.makeText(this, "无法读取图片", Toast.LENGTH_SHORT).show()
                return
            }
            
            saveBitmapToGallery(bitmap)
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun saveBitmapToGallery(bitmap: android.graphics.Bitmap) {
        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "OpenQwens_${System.currentTimeMillis()}.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }
            }
            
            val uri: Uri? = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                val outputStream = contentResolver.openOutputStream(it)
                outputStream?.use { stream ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                    Toast.makeText(this, "图片已保存到相册", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageDetailScreen(
    messageContent: String,
    messageTimestamp: Long,
    isFromUser: Boolean,
    imageUrls: List<String>,
    localImagePaths: List<String>,
    onBackClick: () -> Unit,
    onSaveImage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // 创建一个状态变量来保存可编辑的文本内容
    var editableMessageContent by remember { mutableStateOf(messageContent) }
    var showImagePreview by remember { mutableStateOf(false) }
    var selectedImageIndex by remember { mutableStateOf(0) }

    val timeFormat = SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault())
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    // 图片预览对话框
    if (showImagePreview && (imageUrls.isNotEmpty() || localImagePaths.isNotEmpty())) {
        ImagePreviewDialog(
            imageUrls = imageUrls,
            localImagePaths = localImagePaths,
            initialIndex = selectedImageIndex,
            onDismiss = { showImagePreview = false },
            onSaveImage = onSaveImage
        )
    }

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
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState)
        ) {
            // 显示时间戳
            Text(
                text = timeFormat.format(Date(messageTimestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // 显示文本内容
            if (messageContent.isNotBlank()) {
                Text(
                    text = "文本内容：",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                TextField(
                    value = editableMessageContent,
                    onValueChange = { newValue ->
                        editableMessageContent = newValue
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.4
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
            
            // 显示图片（如果有）
            if (imageUrls.isNotEmpty() || localImagePaths.isNotEmpty()) {
                Text(
                    text = "图片内容：",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp, top = 16.dp)
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    // 优先显示本地图片，如果没有则显示网络图片
                    val imagesToShow = if (localImagePaths.isNotEmpty()) localImagePaths else imageUrls
                    
                    items(imagesToShow.size) { index ->
                        val imageSource = if (localImagePaths.isNotEmpty()) {
                            "file://${localImagePaths[index]}"
                        } else {
                            imageUrls[index]
                        }
                        
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imageSource)
                                .crossfade(true)
                                .build(),
                            contentDescription = "生成的图片 ${index + 1}",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedImageIndex = index
                                    showImagePreview = true
                                },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}