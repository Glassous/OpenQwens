package com.glassous.openqwens.ui.components

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 表示用户选中的功能
 */
data class SelectedFunction(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector
)

/**
 * 功能类型枚举
 */
enum class FunctionType(
    val id: String,
    val displayName: String,
    val description: String
) {
    DEEP_THINKING("deep_thinking", "深度思考", "启用深度推理模式"),
    WEB_SEARCH("web_search", "联网搜索", "搜索最新信息"),
    IMAGE_GENERATION("image_generation", "图片生成", "AI生成图片"),
    IMAGE_EDITING("image_editing", "图片编辑", "编辑和修改图片"),
    VISION_UNDERSTANDING("vision_understanding", "视觉理解", "分析和理解图片内容"),
    
    // 附件类型
    CAMERA("camera", "相机", "拍照或录像"),
    IMAGE("image", "图片", "从相册选择")
}