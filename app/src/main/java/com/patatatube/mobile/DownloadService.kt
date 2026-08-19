package com.patatatube.mobile

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

class DownloadService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val NOTIFICATION_ID = 1234
    private val DESKTOP_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
    
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_DOWNLOAD") {
            stopDownload()
            return START_NOT_STICKY
        }
        
        val url = intent?.getStringExtra("URL") ?: return START_NOT_STICKY
        val type = intent.getStringExtra("TYPE") ?: "Video"
        
        startForeground(NOTIFICATION_ID, createNotification("Patatatube: Descargando $type", "Preparando...", -1, true))
        
        serviceScope.launch {
            var sawHttp403 = false
            try {
                val tmpDir = File(cacheDir, "yt_tmp")
                if (tmpDir.exists()) tmpDir.deleteRecursively()
                tmpDir.mkdirs()
                
                val request = YoutubeDLRequest(url)
                request.addOption("--no-playlist")
                request.addOption("--playlist-items", "1")
                request.addOption("-o", "${tmpDir.absolutePath}/%(title)s.%(ext)s")
                request.addOption("--no-warnings")
                request.addOption("--ignore-errors")
                request.addOption("--user-agent", DESKTOP_USER_AGENT)
                request.addOption("--add-header", "User-Agent: $DESKTOP_USER_AGENT")
                
                if (type == "Audio") {
                    request.addOption("-x")
                    request.addOption("--audio-format", "mp3")
                    request.addOption("--embed-thumbnail")
                    request.addOption("--embed-metadata")
                } else {
                    request.addOption("-f", "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best")
                }
                
                DownloadManager.downloadState.value = DownloadState.DOWNLOADING
                DownloadManager.progress.value = -1f
                DownloadManager.addLog("Starting $type download for: $url")
                
                YoutubeDL.getInstance().execute(request, "patataProcess") { p, _, line ->
                    DownloadManager.progress.value = p / 100f
                    updateNotification("Patatatube: Descargando $type", "Progreso: ${p.toInt()}%", p.toInt(), false)
                    if (!line.isNullOrBlank()) {
                        if (line.contains("403", ignoreCase = true) || line.contains("forbidden", ignoreCase = true)) {
                            sawHttp403 = true
                        }
                        DownloadManager.addLog(line)
                    }
                }
                
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val patataDir = File(downloadsDir, "patatatube")
                if (!patataDir.exists()) patataDir.mkdirs()
                
                val downloadedFiles = tmpDir.listFiles()
                var finalFile: File? = null
                if (downloadedFiles != null) {
                    for (file in downloadedFiles) {
                        val dest = File(patataDir, file.name)
                        file.copyTo(dest, overwrite = true)
                        finalFile = dest
                    }
                }
                if (finalFile == null) {
                    throw IllegalStateException("No se generó ningún archivo de salida.")
                }
                tmpDir.deleteRecursively()
                
                DownloadManager.progress.value = 1f
                DownloadManager.lastDownloadedFilePath.value = finalFile?.absolutePath
                DownloadManager.downloadState.value = DownloadState.FINISHED
                DownloadManager.addLog("¡Descarga completada en Descargas/patatatube!")
                updateNotification("¡Descarga Completada!", finalFile?.name ?: "Archivo guardado", 100, false)
                
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()
                
            } catch (e: Exception) {
                if (e.message?.contains("Process destroyed") == true) return@launch
                DownloadManager.downloadState.value = DownloadState.ERROR
                val isHttp403 = sawHttp403 ||
                    e.message?.contains("403", ignoreCase = true) == true ||
                    e.message?.contains("forbidden", ignoreCase = true) == true
                if (isHttp403) {
                    DownloadManager.addLog("Error 403 Forbidden: YouTube/TikTok rechazó la solicitud HTTP.")
                    DownloadManager.addLog("Sugerencia: intenta actualizar yt-dlp y usa cookies/proxy si el bloqueo persiste.")
                    updateNotification("Error 403", "Bloqueo del servidor (prueba cookies/proxy).", -1, false)
                } else {
                    DownloadManager.addLog("Error crítico: El enlace no es válido o ha sido rechazado por yt-dlp.")
                    updateNotification("Error de descarga", "Enlace no válido o rechazado.", -1, false)
                }
                DownloadManager.addLog("Detalle: ${e.message}")
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()
            }
        }
        
        return START_STICKY
    }
    
    private fun stopDownload() {
        YoutubeDL.getInstance().destroyProcessById("patataProcess")
        File(cacheDir, "yt_tmp").deleteRecursively()
        DownloadManager.reset()
        DownloadManager.addLog("Download stopped by user.")
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.cancel(NOTIFICATION_ID)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    private fun createNotification(title: String, body: String, prog: Int, indeterminate: Boolean): android.app.Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("PATATA_CHANNEL", "Descargas Patatatube", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
        
        val builder = NotificationCompat.Builder(this, "PATATA_CHANNEL")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(prog in 0..99 || indeterminate)
            
        if (prog >= 0) builder.setProgress(100, prog, false)
        else if (indeterminate) builder.setProgress(0, 0, true)
        else builder.setProgress(0, 0, false)
        
        return builder.build()
    }
    
    private fun updateNotification(title: String, body: String, prog: Int, indeterminate: Boolean) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, createNotification(title, body, prog, indeterminate))
    }
}
