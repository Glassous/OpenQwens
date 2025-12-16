package com.glassous.openqwens.data

/**
 * 图片生成参数
 */
data class ImageGenerationParams(
    val resolution: String = "1024*1024",
    val style: String = "<auto>"
) {
    companion object {
        val RESOLUTIONS = listOf(
            "1024*1024",
            "1280*720",
            "720*1280",
            "1024*768",
            "768*1024",
            "512*512"
        ).distinct()

        val STYLES = listOf(
            Pair("<auto>", "自动"),
            Pair("photography", "摄影"),
            Pair("portrait", "人像"),
            Pair("3d cartoon", "3D卡通"),
            Pair("anime", "动漫"),
            Pair("oil painting", "油画"),
            Pair("watercolor", "水彩"),
            Pair("sketch", "素描"),
            Pair("chinese painting", "国画"),
            Pair("flat illustration", "扁平插画")
        )
    }
}

/**
 * 视频生成参数
 */
data class VideoGenerationParams(
    val resolution: String = "720P",
    val duration: Int = 5
) {
    companion object {
        val RESOLUTIONS = listOf(
            "1080P",
            "720P",
            "480P"
        )
        
        val DURATIONS = listOf(
            5,
            10
        )
    }
}
