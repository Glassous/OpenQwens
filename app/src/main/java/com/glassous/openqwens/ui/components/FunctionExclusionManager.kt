package com.glassous.openqwens.ui.components

/**
 * 功能互斥管理器
 * 根据用户需求定义功能间的互斥规则
 */
object FunctionExclusionManager {
    
    /**
     * 获取指定功能类型应该被禁用的功能列表
     * @param selectedType 已选择的功能类型
     * @return 应该被禁用的功能类型列表
     */
    fun getExcludedFunctions(selectedType: FunctionType): Set<FunctionType> {
        return when (selectedType) {
            // 1. 上传图片（包括相机）后，禁用视频，音频，文件，联网搜索，图片生成，视频生成
            FunctionType.IMAGE, FunctionType.CAMERA -> setOf(
                FunctionType.VIDEO,
                FunctionType.AUDIO,
                FunctionType.FILE,
                FunctionType.WEB_SEARCH,
                FunctionType.IMAGE_GENERATION,
                FunctionType.VIDEO_GENERATION
            )
            
            // 2. 上传视频后，禁用相机，图片，音频，文件，联网搜索，图片生成，图片编辑，视频生成
            FunctionType.VIDEO -> setOf(
                FunctionType.CAMERA,
                FunctionType.IMAGE,
                FunctionType.AUDIO,
                FunctionType.FILE,
                FunctionType.WEB_SEARCH,
                FunctionType.IMAGE_GENERATION,
                FunctionType.IMAGE_EDITING,
                FunctionType.VIDEO_GENERATION
            )
            
            // 3. 上传音频后，禁用全部，包括音频
            FunctionType.AUDIO -> FunctionType.values().toSet()
            
            // 4. 上传文件后，禁用全部，包括文件
            FunctionType.FILE -> FunctionType.values().toSet()
            
            // 5. 选择深度思考后，禁用音频，文件，联网搜索，图片生成，图片编辑，视频生成
            FunctionType.DEEP_THINKING -> setOf(
                FunctionType.AUDIO,
                FunctionType.FILE,
                FunctionType.WEB_SEARCH,
                FunctionType.IMAGE_GENERATION,
                FunctionType.IMAGE_EDITING,
                FunctionType.VIDEO_GENERATION
            )
            
            // 6. 选择联网搜索后，禁用全部，包括联网搜索
            FunctionType.WEB_SEARCH -> FunctionType.values().toSet()
            
            // 7. 选择图片生成后，禁用全部，包括图片生成
            FunctionType.IMAGE_GENERATION -> FunctionType.values().toSet()
            
            // 8. 选择图片编辑后，禁用视频，音频，文件，深度思考，联网搜索，图片生成，图片编辑，视频生成
            FunctionType.IMAGE_EDITING -> setOf(
                FunctionType.VIDEO,
                FunctionType.AUDIO,
                FunctionType.FILE,
                FunctionType.DEEP_THINKING,
                FunctionType.WEB_SEARCH,
                FunctionType.IMAGE_GENERATION,
                FunctionType.IMAGE_EDITING,
                FunctionType.VIDEO_GENERATION
            )
            
            // 9. 视频生成直接禁用（SDK暂不支持）
            FunctionType.VIDEO_GENERATION -> FunctionType.values().toSet()
        }
    }
    
    /**
     * 检查功能是否应该被禁用
     * @param functionType 要检查的功能类型
     * @param selectedFunctions 已选择的功能列表
     * @param selectedAttachments 已选择的附件列表
     * @return true 如果应该被禁用
     */
    fun shouldBeDisabled(
        functionType: FunctionType,
        selectedFunctions: List<SelectedFunction>,
        selectedAttachments: List<AttachmentData>
    ): Boolean {
        // 9. 视频生成直接禁用（SDK暂不支持）
        if (functionType == FunctionType.VIDEO_GENERATION) {
            return true
        }
        
        // 检查已选择的功能是否排斥当前功能
        for (selectedFunction in selectedFunctions) {
            val selectedType = FunctionType.values().find { it.id == selectedFunction.id }
            if (selectedType != null) {
                val excludedFunctions = getExcludedFunctions(selectedType)
                if (functionType in excludedFunctions) {
                    return true
                }
            }
        }
        
        // 检查已选择的附件是否排斥当前功能
        for (attachment in selectedAttachments) {
            val attachmentType = when {
                attachment.mimeType.startsWith("image/") -> FunctionType.IMAGE
                attachment.mimeType.startsWith("video/") -> FunctionType.VIDEO
                attachment.mimeType.startsWith("audio/") -> FunctionType.AUDIO
                else -> FunctionType.FILE
            }
            
            val excludedFunctions = getExcludedFunctions(attachmentType)
            if (functionType in excludedFunctions) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * 获取所有被禁用的功能类型
     * @param selectedFunctions 已选择的功能列表
     * @param selectedAttachments 已选择的附件列表
     * @return 被禁用的功能类型集合
     */
    fun getAllDisabledFunctions(
        selectedFunctions: List<SelectedFunction>,
        selectedAttachments: List<AttachmentData>
    ): Set<FunctionType> {
        val disabledFunctions = mutableSetOf<FunctionType>()
        
        // 视频生成直接禁用
        disabledFunctions.add(FunctionType.VIDEO_GENERATION)
        
        // 根据已选择的功能添加禁用项
        for (selectedFunction in selectedFunctions) {
            val selectedType = FunctionType.values().find { it.id == selectedFunction.id }
            if (selectedType != null) {
                disabledFunctions.addAll(getExcludedFunctions(selectedType))
            }
        }
        
        // 根据已选择的附件添加禁用项
        for (attachment in selectedAttachments) {
            val attachmentType = when {
                attachment.mimeType.startsWith("image/") -> FunctionType.IMAGE
                attachment.mimeType.startsWith("video/") -> FunctionType.VIDEO
                attachment.mimeType.startsWith("audio/") -> FunctionType.AUDIO
                else -> FunctionType.FILE
            }
            disabledFunctions.addAll(getExcludedFunctions(attachmentType))
        }
        
        return disabledFunctions
    }
}