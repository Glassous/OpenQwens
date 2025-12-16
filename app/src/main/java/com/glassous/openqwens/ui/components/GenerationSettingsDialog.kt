package com.glassous.openqwens.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.glassous.openqwens.data.ImageGenerationParams
import com.glassous.openqwens.data.VideoGenerationParams

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerationSettingsDialog(
    isImageGeneration: Boolean,
    isVideoGeneration: Boolean,
    imageParams: ImageGenerationParams,
    videoParams: VideoGenerationParams,
    onImageParamsChange: (ImageGenerationParams) -> Unit,
    onVideoParamsChange: (VideoGenerationParams) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "生成参数设置",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            if (isImageGeneration) {
                Text(
                    text = "图片生成设置",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 图片分辨率
                Text(
                    text = "分辨率",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ImageGenerationParams.RESOLUTIONS.take(2).forEach { res ->
                        FilterChip(
                            selected = imageParams.resolution == res,
                            onClick = { onImageParamsChange(imageParams.copy(resolution = res)) },
                            label = { Text(res) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ImageGenerationParams.RESOLUTIONS.drop(2).forEach { res ->
                        FilterChip(
                            selected = imageParams.resolution == res,
                            onClick = { onImageParamsChange(imageParams.copy(resolution = res)) },
                            label = { Text(res) }
                        )
                    }
                }

                // 图片风格
                Text(
                    text = "风格",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // 风格比较多，用流式布局或者简单的几行
                // 这里简单处理，显示前几个，更多选项可以用Dropdown或者更复杂的UI
                // 为了简单起见，这里列出常用的
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                     ImageGenerationParams.STYLES.chunked(3).forEach { rowStyles ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowStyles.forEach { (styleValue, styleName) ->
                                FilterChip(
                                    selected = imageParams.style == styleValue,
                                    onClick = { onImageParamsChange(imageParams.copy(style = styleValue)) },
                                    label = { Text(styleName) }
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (isVideoGeneration) {
                if (isImageGeneration) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                }
                
                Text(
                    text = "视频生成设置",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 视频分辨率
                Text(
                    text = "分辨率",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VideoGenerationParams.RESOLUTIONS.forEach { res ->
                        FilterChip(
                            selected = videoParams.resolution == res,
                            onClick = { onVideoParamsChange(videoParams.copy(resolution = res)) },
                            label = { Text(res) }
                        )
                    }
                }
                
                // 视频时长
                Text(
                    text = "时长",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VideoGenerationParams.DURATIONS.forEach { duration ->
                        FilterChip(
                            selected = videoParams.duration == duration,
                            onClick = { onVideoParamsChange(videoParams.copy(duration = duration)) },
                            label = { Text("${duration}秒") }
                        )
                    }
                }
            }
        }
    }
}
