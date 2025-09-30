package com.glassous.openqwens.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImagePreviewDialog(
    imageUrls: List<String>,
    localImagePaths: List<String>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    onSaveImage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 优先使用本地图片，如果没有则使用网络图片
    val imagesToShow = if (localImagePaths.isNotEmpty()) localImagePaths else imageUrls
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { imagesToShow.size }
    )
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
        ) {
            // 关闭按钮
            IconButton(
                onClick = {
                    println("关闭按钮被点击") // 调试日志
                    onDismiss()
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(48.dp) // 增加按钮大小
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // 保存按钮
            IconButton(
                onClick = {
                    println("保存按钮被点击") // 调试日志
                    val currentImagePath = if (localImagePaths.isNotEmpty()) {
                        localImagePaths[pagerState.currentPage]
                    } else if (imageUrls.isNotEmpty()) {
                        imageUrls[pagerState.currentPage]
                    } else {
                        return@IconButton
                    }
                    onSaveImage(currentImagePath)
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .size(48.dp) // 增加按钮大小
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "保存到相册",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // 图片轮播
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 64.dp) // 为顶部按钮留出空间
            ) { page ->
                val imageSource = if (localImagePaths.isNotEmpty()) {
                    "file://${localImagePaths[page]}"
                } else {
                    imageUrls[page]
                }
                
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imageSource)
                            .crossfade(true)
                            .build(),
                        contentDescription = "预览图片 ${page + 1}",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            
            // 页面指示器（如果有多张图片）
            if (imagesToShow.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(imagesToShow.size) { index ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = if (index == pagerState.currentPage) Color.White else Color.White.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}