package com.glassous.openqwens.utils

import android.content.Context
import android.net.Uri
import com.glassous.openqwens.data.ChatRepository
import com.glassous.openqwens.data.ChatSession
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupHelper(private val context: Context) {
    private val repository = ChatRepository(context)
    private val gson = Gson()

    suspend fun exportData(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        val tempDir = File(context.cacheDir, "backup_temp").apply {
            if (exists()) deleteRecursively()
            mkdirs()
        }
        val mediaDir = File(tempDir, "media").apply { mkdirs() }

        try {
            val sessions = repository.loadSessions()
            
            // Transform sessions to use relative paths and copy media files
            val backupSessions = sessions.map { session ->
                session.copy(messages = session.messages.map { message ->
                    // Handle Images
                    val newLocalImagePaths = message.localImagePaths.mapNotNull { absolutePath ->
                        try {
                            val sourceFile = File(absolutePath)
                            if (sourceFile.exists()) {
                                val fileName = sourceFile.name
                                sourceFile.copyTo(File(mediaDir, fileName), overwrite = true)
                                "media/$fileName"
                            } else {
                                null
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            null
                        }
                    }

                    // Handle Video
                    val newLocalVideoPath = message.localVideoPath?.let { absolutePath ->
                        try {
                            val sourceFile = File(absolutePath)
                            if (sourceFile.exists()) {
                                val fileName = sourceFile.name
                                sourceFile.copyTo(File(mediaDir, fileName), overwrite = true)
                                "media/$fileName"
                            } else {
                                null
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            null
                        }
                    }

                    message.copy(
                        localImagePaths = newLocalImagePaths,
                        localVideoPath = newLocalVideoPath
                    )
                })
            }

            // Save JSON
            val json = gson.toJson(backupSessions)
            File(tempDir, "chat_data.json").writeText(json)

            // Save Config
            val prefs = context.getSharedPreferences("dashscope_config", Context.MODE_PRIVATE)
            val config = mapOf(
                "base_url" to prefs.getString("base_url", null),
                "api_keys" to prefs.getString("api_keys", null),
                "selected_api_key_id" to prefs.getString("selected_api_key_id", null),
                "models" to prefs.getString("models", null),
                "selected_model_id" to prefs.getString("selected_model_id", null),
                "selected_model_id_TEXT" to prefs.getString("selected_model_id_TEXT", null),
                "selected_model_id_IMAGE" to prefs.getString("selected_model_id_IMAGE", null),
                "selected_model_id_VIDEO" to prefs.getString("selected_model_id_VIDEO", null)
            )
            val configJson = gson.toJson(config)
            File(tempDir, "config.json").writeText(configJson)

            // Zip everything
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
                    zipDirectory(tempDir, tempDir, zipOut)
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    suspend fun importData(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        val tempDir = File(context.cacheDir, "restore_temp").apply {
            if (exists()) deleteRecursively()
            mkdirs()
        }

        try {
            // Unzip file
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                unzip(inputStream, tempDir)
            }

            // Restore Config
            val configFile = File(tempDir, "config.json")
            if (configFile.exists()) {
                val configJson = configFile.readText()
                val type = object : TypeToken<Map<String, String?>>() {}.type
                val config: Map<String, String?> = gson.fromJson(configJson, type)
                
                val prefs = context.getSharedPreferences("dashscope_config", Context.MODE_PRIVATE)
                val editor = prefs.edit()
                
                config["base_url"]?.let { editor.putString("base_url", it) }
                config["api_keys"]?.let { editor.putString("api_keys", it) }
                config["selected_api_key_id"]?.let { editor.putString("selected_api_key_id", it) }
                config["models"]?.let { editor.putString("models", it) }
                config["selected_model_id"]?.let { editor.putString("selected_model_id", it) }
                config["selected_model_id_TEXT"]?.let { editor.putString("selected_model_id_TEXT", it) }
                config["selected_model_id_IMAGE"]?.let { editor.putString("selected_model_id_IMAGE", it) }
                config["selected_model_id_VIDEO"]?.let { editor.putString("selected_model_id_VIDEO", it) }
                
                editor.apply()
            }

            val jsonFile = File(tempDir, "chat_data.json")
            if (!jsonFile.exists()) {
                // Fallback for old legacy JSON-only backups
                // Try to parse the input stream directly as JSON if unzip failed or file missing?
                // But we already consumed the stream.
                // Assuming the user is selecting a ZIP file now.
                // If it's a legacy JSON file, unzip might fail or produce nothing.
                // Let's try to handle legacy JSON backup if zip fails?
                // For now, let's assume standard flow. If jsonFile is missing, maybe it was a raw JSON file.
                
                // Retry as raw JSON
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val content = InputStreamReader(inputStream).readText()
                     if (content.trim().startsWith("[")) {
                         // It's likely a legacy JSON backup
                         File(tempDir, "chat_data.json").writeText(content)
                     } else {
                         return@withContext Result.failure(Exception("无效的备份文件"))
                     }
                }
            }
            
            if (!File(tempDir, "chat_data.json").exists()) {
                 return@withContext Result.failure(Exception("找不到数据文件"))
            }

            val json = File(tempDir, "chat_data.json").readText()
            val type = object : TypeToken<List<ChatSession>>() {}.type
            val importedSessions: List<ChatSession> = gson.fromJson(json, type)

            if (importedSessions.isEmpty()) {
                return@withContext Result.success(0)
            }

            // Ensure app media directories exist
            val appImagesDir = File(context.filesDir, "chat_images").apply { mkdirs() }
            val appVideosDir = File(context.filesDir, "chat_videos").apply { mkdirs() }

            // Restore media and update paths
            val finalSessions = importedSessions.map { session ->
                session.copy(messages = session.messages.map { message ->
                    // Restore Images
                    val restoredImagePaths = message.localImagePaths.mapNotNull { path ->
                        if (path.startsWith("media/")) {
                            val fileName = File(path).name
                            val backupFile = File(tempDir, path) // e.g. temp/media/xyz.png
                            if (backupFile.exists()) {
                                val targetFile = File(appImagesDir, fileName)
                                backupFile.copyTo(targetFile, overwrite = true)
                                targetFile.absolutePath
                            } else {
                                null
                            }
                        } else {
                            // Legacy absolute path or other? Keep if exists, else drop?
                            // If it's an absolute path from another device, it won't exist.
                            // If it's a legacy backup on same device, it might exist.
                            if (File(path).exists()) path else null
                        }
                    }

                    // Restore Video
                    val restoredVideoPath = message.localVideoPath?.let { path ->
                         if (path.startsWith("media/")) {
                            val fileName = File(path).name
                            val backupFile = File(tempDir, path)
                            if (backupFile.exists()) {
                                val targetFile = File(appVideosDir, fileName)
                                backupFile.copyTo(targetFile, overwrite = true)
                                targetFile.absolutePath
                            } else {
                                null
                            }
                        } else {
                            if (File(path).exists()) path else null
                        }
                    }

                    message.copy(
                        localImagePaths = restoredImagePaths,
                        localVideoPath = restoredVideoPath
                    )
                })
            }

            // Merge with existing sessions
            val existingSessions = repository.loadSessions().toMutableList()
            var addedCount = 0
            var updatedCount = 0
            
            for (session in finalSessions) {
                val index = existingSessions.indexOfFirst { it.id == session.id }
                if (index != -1) {
                    existingSessions[index] = session
                    updatedCount++
                } else {
                    existingSessions.add(session)
                    addedCount++
                }
            }
            
            repository.saveSessions(existingSessions)
            Result.success(addedCount + updatedCount)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private fun zipDirectory(rootDir: File, sourceDir: File, out: ZipOutputStream) {
        val files = sourceDir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                zipDirectory(rootDir, file, out)
            } else {
                val entryName = file.absolutePath.substring(rootDir.absolutePath.length + 1)
                val entry = ZipEntry(entryName)
                out.putNextEntry(entry)
                FileInputStream(file).use { origin ->
                    origin.copyTo(out)
                }
                out.closeEntry()
            }
        }
    }

    private fun unzip(inputStream: java.io.InputStream, targetDir: File) {
        ZipInputStream(BufferedInputStream(inputStream)).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                val file = File(targetDir, entry.name)
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).use { fos ->
                        zipIn.copyTo(fos)
                    }
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }
    }
}
