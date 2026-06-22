package com.patatatube.mobile

import kotlinx.coroutines.flow.MutableStateFlow

enum class DownloadState {
    IDLE, DOWNLOADING, FINISHED, ERROR
}

object DownloadManager {
    val url = MutableStateFlow("")
    val downloadState = MutableStateFlow(DownloadState.IDLE)
    val progress = MutableStateFlow(-1f)
    val logs = MutableStateFlow(listOf("YoutubeDL & FFmpeg initialized. Ready."))
    val lastDownloadedFilePath = MutableStateFlow<String?>(null)
    
    fun addLog(log: String) {
        logs.value = logs.value + log
    }
    
    fun reset() {
        downloadState.value = DownloadState.IDLE
        progress.value = -1f
        lastDownloadedFilePath.value = null
    }
}
