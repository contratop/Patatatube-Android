package com.patatatube.mobile

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.OndemandVideo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patatatube.mobile.ui.theme.AppThemeMode
import com.patatatube.mobile.ui.theme.PatatatubeMobileTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.ui.graphics.TransformOrigin
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.ffmpeg.FFmpeg
import android.os.Environment
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.content.FileProvider
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import android.content.Intent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.Manifest
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable

enum class UpdateCheckState { IDLE, LOADING, AVAILABLE, UP_TO_DATE, ERROR }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            YoutubeDL.getInstance().init(application)
            FFmpeg.getInstance().init(application)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("PATATA_CHANNEL", "Descargas Patatatube", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
        
        val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        var sharedText = ""
        if (intent?.action == android.content.Intent.ACTION_SEND && intent.type == "text/plain") {
            sharedText = intent.getStringExtra(android.content.Intent.EXTRA_TEXT) ?: ""
        }

        setContent {
            var currentTheme by rememberSaveable { mutableStateOf(AppThemeMode.DARK) }
            
            // Global State from DownloadManager
            val url by DownloadManager.url.collectAsState()
            val downloadState by DownloadManager.downloadState.collectAsState()
            val progress by DownloadManager.progress.collectAsState()
            val logs by DownloadManager.logs.collectAsState()
            
            var showTerminal by rememberSaveable { mutableStateOf(false) }
            val context = LocalContext.current
            
            if (sharedText.isNotBlank() && url.isBlank()) {
                LaunchedEffect(sharedText) {
                    DownloadManager.url.value = sharedText
                }
            }

            
            PatatatubeMobileTheme(appThemeMode = currentTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PatatatubeScreen(
                        currentTheme = currentTheme,
                        onThemeToggle = {
                            currentTheme = when (currentTheme) {
                                AppThemeMode.DARK -> AppThemeMode.LIGHT
                                AppThemeMode.LIGHT -> AppThemeMode.POKE
                                AppThemeMode.POKE -> AppThemeMode.DARK
                            }
                        },
                        url = url,
                        onUrlChange = { DownloadManager.url.value = it },
                        downloadState = downloadState,
                        onDownloadStateChange = { DownloadManager.downloadState.value = it },
                        progress = progress,
                        onProgressChange = { DownloadManager.progress.value = it },
                        logs = logs,
                        showTerminal = showTerminal,
                        onShowTerminalChange = { showTerminal = it }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PatatatubeScreen(
    currentTheme: AppThemeMode, 
    onThemeToggle: () -> Unit,
    url: String,
    onUrlChange: (String) -> Unit,
    downloadState: DownloadState,
    onDownloadStateChange: (DownloadState) -> Unit,
    progress: Float,
    onProgressChange: (Float) -> Unit,
    logs: List<String>,
    showTerminal: Boolean,
    onShowTerminalChange: (Boolean) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val lastDownloadedFilePath by DownloadManager.lastDownloadedFilePath.collectAsState()
    val lastDownloadedFile = lastDownloadedFilePath?.let { File(it) }
    var showCredits by remember { mutableStateOf(false) }

    var showUpdateModal by remember { mutableStateOf(false) }
    var updateCheckState by remember { mutableStateOf(UpdateCheckState.IDLE) }
    var latestReleaseName by remember { mutableStateOf("") }
    var latestReleaseUrl by remember { mutableStateOf("") }

    fun checkUpdateFromGithub() {
        updateCheckState = UpdateCheckState.LOADING
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val currentVersion = packageInfo.versionName ?: "1.0.0"
                
                val urlObj = java.net.URL("https://api.github.com/repos/contratop/Patatatube-Android/releases/latest")
                val connection = urlObj.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val json = org.json.JSONObject(response)
                    val tagName = json.getString("tag_name").replace("v", "")
                    val name = json.getString("name")
                    val htmlUrl = json.getString("html_url")
                    
                    withContext(Dispatchers.Main) {
                        if (tagName != currentVersion) {
                            latestReleaseName = name
                            latestReleaseUrl = htmlUrl
                            updateCheckState = UpdateCheckState.AVAILABLE
                        } else {
                            updateCheckState = UpdateCheckState.UP_TO_DATE
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) { updateCheckState = UpdateCheckState.ERROR }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { updateCheckState = UpdateCheckState.ERROR }
            }
        }
    }

    fun updateYtdlp() {
        coroutineScope.launch(Dispatchers.Main) {
            Toast.makeText(context, "Buscando actualizaciones de yt-dlp...", Toast.LENGTH_SHORT).show()
        }
        coroutineScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { DownloadManager.addLog("Checking for yt-dlp updates...") }
                val status = YoutubeDL.getInstance().updateYoutubeDL(context, YoutubeDL.UpdateChannel.STABLE)
                withContext(Dispatchers.Main) { DownloadManager.addLog("Update result: $status") }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { DownloadManager.addLog("Update error: ${e.message}") }
            }
        }
    }
    
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("patatatube_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("is_first_run", true)) {
            prefs.edit().putBoolean("is_first_run", false).apply()
            updateYtdlp()
        }
    }
    
    // Real Download Logic
    fun startDownload(type: String) {
        if (url.isBlank()) {
            Toast.makeText(context, "¡Pon URL antes!", Toast.LENGTH_SHORT).show()
            DownloadManager.addLog("Error: URL is empty.")
            return
        }
        val intent = Intent(context, DownloadService::class.java).apply {
            putExtra("URL", url)
            putExtra("TYPE", type)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            
            // Title
            Text(
                text = "Patatatube",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 56.sp
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // URL Input
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                label = { 
                    Text(
                        if (currentTheme == AppThemeMode.POKE) "✨ Pega aqui tu enlace magico 🌸"
                        else "Deja caer tu enlace aquí..."
                    ) 
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.secondary,
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Actions Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 70.dp),
                contentAlignment = Alignment.Center
            ) {
                if (downloadState == DownloadState.IDLE) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Download Audio
                        IconButton(
                            onClick = { startDownload("Audio") },
                            modifier = Modifier
                                .size(64.dp)
                                .border(4.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Audiotrack,
                                contentDescription = "Download Audio",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        
                        // Download Video
                        IconButton(
                            onClick = { startDownload("Video") },
                            modifier = Modifier
                                .size(64.dp)
                                .border(4.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.OndemandVideo,
                                contentDescription = "Download Video",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(24.dp)
                                    .border(4.dp, if (downloadState == DownloadState.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, CircleShape)
                                    .clip(CircleShape)
                            ) {
                                if (progress < 0f) {
                                    LinearProgressIndicator(
                                        modifier = Modifier.fillMaxSize(),
                                        color = if (downloadState == DownloadState.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                        trackColor = Color.Transparent
                                    )
                                } else {
                                    LinearProgressIndicator(
                                        progress = progress,
                                        modifier = Modifier.fillMaxSize(),
                                        color = if (downloadState == DownloadState.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                        trackColor = Color.Transparent
                                    )
                                }
                            }
                            
                            if (downloadState == DownloadState.DOWNLOADING || downloadState == DownloadState.ERROR) {
                                IconButton(
                                    onClick = { 
                                        if (downloadState == DownloadState.DOWNLOADING) {
                                            val stopIntent = Intent(context, DownloadService::class.java).apply { action = "STOP_DOWNLOAD" }
                                            context.startService(stopIntent)
                                        } else {
                                            DownloadManager.reset()
                                        }
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .border(4.dp, if (downloadState == DownloadState.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = if (downloadState == DownloadState.ERROR) Icons.Default.Close else Icons.Default.Stop,
                                        contentDescription = "Stop or Close",
                                        tint = if (downloadState == DownloadState.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (downloadState == DownloadState.ERROR) "Error. Revisa la terminal" 
                                   else if (progress < 0f) "Preparando..." 
                                   else "${(progress * 100).toInt()}%",
                            color = if (downloadState == DownloadState.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
            
            AnimatedVisibility(visible = downloadState == DownloadState.FINISHED) {
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { 
                            DownloadManager.reset()
                            DownloadManager.url.value = ""
                            NotificationManagerCompat.from(context).cancel(1234)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Nueva")
                    }
                    
                    Button(
                        onClick = { 
                            onDownloadStateChange(DownloadState.IDLE)
                            lastDownloadedFile?.let { file ->
                                try {
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                                    intent.setDataAndType(uri, "*/*")
                                    intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    DownloadManager.addLog("No se pudo abrir el archivo: ${e.message}")
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Abrir")
                    }
                    
                    Button(
                        onClick = { 
                            lastDownloadedFile?.let { file ->
                                try {
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND)
                                    intent.type = "*/*"
                                    intent.putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                    intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    context.startActivity(android.content.Intent.createChooser(intent, "Compartir con"))
                                } catch (e: Exception) {
                                    DownloadManager.addLog("Error al compartir: ${e.message}")
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Compartir")
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Embedded Terminal
            AnimatedVisibility(visible = showTerminal) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Debug Terminal",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    ) {
                        logs.forEach { log ->
                            Text(
                                text = "> $log",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        LaunchedEffect(logs.size) {
                            scrollState.animateScrollTo(scrollState.maxValue)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(72.dp)) // Extra space so FABs don't overlap
        }

        // Version Number (Bottom Center)
        val packageInfo = try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (e: Exception) { null }
        val versionName = packageInfo?.versionName ?: "1.0.0"

        Text(
            text = "v$versionName",
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .combinedClickable(
                    onClick = {},
                    onLongClick = {
                        showUpdateModal = true
                        updateCheckState = UpdateCheckState.IDLE
                    }
                )
        )

        // Terminal Toggle FAB (Bottom Left)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface, CircleShape)
                .combinedClickable(
                    onClick = { onShowTerminalChange(!showTerminal) },
                    onLongClick = { updateYtdlp() }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.Terminal, contentDescription = "Toggle Terminal", tint = MaterialTheme.colorScheme.primary)
        }

        if (showCredits) {
            AlertDialog(
                onDismissRequest = { showCredits = false },
                title = { Text("Créditos") },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Image(
                                painter = painterResource(id = R.drawable.pokeinalover),
                                contentDescription = "pokeinalover",
                                modifier = Modifier.size(64.dp).clip(CircleShape)
                            )
                            Text("Diseñado por pokeinalover")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Image(
                                painter = painterResource(id = R.drawable.contratop),
                                contentDescription = "ContratopDev",
                                modifier = Modifier.size(64.dp).clip(CircleShape)
                            )
                            Text("Programado por ContratopDev")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCredits = false }) { Text("Cerrar") }
                }
            )
        }

        if (showUpdateModal) {
            AlertDialog(
                onDismissRequest = { 
                    if (updateCheckState != UpdateCheckState.LOADING) {
                        showUpdateModal = false 
                    }
                },
                title = { 
                    Text(
                        when(updateCheckState) {
                            UpdateCheckState.IDLE -> "Buscar Actualización"
                            UpdateCheckState.LOADING -> "Buscando..."
                            UpdateCheckState.AVAILABLE -> "¡Nueva Versión!"
                            UpdateCheckState.UP_TO_DATE -> "Actualizado"
                            UpdateCheckState.ERROR -> "Error"
                        }
                    ) 
                },
                text = {
                    when(updateCheckState) {
                        UpdateCheckState.IDLE -> Text("¿Quieres buscar la última versión en GitHub?")
                        UpdateCheckState.LOADING -> {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        UpdateCheckState.AVAILABLE -> Text("Hay una nueva versión disponible: $latestReleaseName\n\n¿Quieres descargarla ahora?")
                        UpdateCheckState.UP_TO_DATE -> Text("Ya tienes la última versión instalada.")
                        UpdateCheckState.ERROR -> Text("Hubo un problema al conectar con GitHub. Inténtalo más tarde.")
                    }
                },
                confirmButton = {
                    if (updateCheckState == UpdateCheckState.IDLE) {
                        TextButton(onClick = { checkUpdateFromGithub() }) { Text("Confirmar") }
                    } else if (updateCheckState == UpdateCheckState.AVAILABLE) {
                        TextButton(onClick = {
                            val urlIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(latestReleaseUrl))
                            context.startActivity(urlIntent)
                            showUpdateModal = false
                        }) { Text("Descargar") }
                    } else if (updateCheckState != UpdateCheckState.LOADING) {
                        TextButton(onClick = { showUpdateModal = false }) { Text("Cerrar") }
                    }
                },
                dismissButton = {
                    if (updateCheckState == UpdateCheckState.IDLE || updateCheckState == UpdateCheckState.AVAILABLE) {
                        TextButton(onClick = { showUpdateModal = false }) { Text("Cancelar") }
                    }
                }
            )
        }

        // Theme Toggle FAB (Bottom Right)
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            shadowElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(56.dp)
                .clip(CircleShape)
                .combinedClickable(
                    onClick = onThemeToggle,
                    onLongClick = { showCredits = true }
                )
        ) {
            Box(contentAlignment = Alignment.Center) {
                val icon = when (currentTheme) {
                    AppThemeMode.DARK -> Icons.Default.DarkMode
                    AppThemeMode.LIGHT -> Icons.Default.LightMode
                    AppThemeMode.POKE -> Icons.Default.Favorite
                }
                Icon(imageVector = icon, contentDescription = "Toggle Theme")
            }
        }
    }
}
