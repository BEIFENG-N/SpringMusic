package com.example.lxn2

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.lxn2.data.MediaRepository
import com.example.lxn2.service.PlaybackService
import com.example.lxn2.ui.theme.LXNTheme
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController by mutableStateOf<MediaController?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LXNTheme {
                MainScreen(mediaController)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture?.addListener({
            mediaController = controllerFuture?.get()
        }, MoreExecutors.directExecutor())
    }

    override fun onStop() {
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
        mediaController = null
        super.onStop()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(controller: MediaController?) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(emptyList<MediaItem>()) }
    var isSearching by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    val manageFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            hasPermission = Environment.isExternalStorageManager()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                manageFilesLauncher.launch(intent)
            } else {
                launcher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    // Online Search Effect
    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            searchResults = emptyList()
            return@LaunchedEffect
        }
        isSearching = true
        val client = OkHttpClient()
        withContext(Dispatchers.IO) {
            try {
                val encodedKeyword = java.net.URLEncoder.encode(searchQuery, "UTF-8")
                val searchUrl = "http://search.kuwo.cn/r.s?client=kt&all=$encodedKeyword&pn=0&rn=20&uid=794762570&ver=kwplayer_ar_9.2.2.1&vipver=1&show_copyright_off=1&newver=1&ft=music&encoding=utf8&rformat=json&mobi=1"
                
                val request = Request.Builder().url(searchUrl).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                
                val cleanJson = body?.replace("'", "\"") ?: "{}"
                val json = JSONObject(cleanJson)
                val songs = json.optJSONArray("abslist")
                val results = mutableListOf<MediaItem>()
                if (songs != null) {
                    for (i in 0 until songs.length()) {
                        val s = songs.getJSONObject(i)
                        val songId = s.optString("MUSICRID").replace("MUSIC_", "")
                        val artist = s.optString("ARTIST") ?: "Unknown"
                        val metadata = MediaMetadata.Builder()
                            .setTitle(s.optString("SONGNAME"))
                            .setArtist(artist)
                            .build()
                        
                        val playUrl = "http://antiserver.kuwo.cn/anti.s?format=mp3&rid=MUSIC_$songId&type=convert_url&response=url&bitrate=128"
                        
                        results.add(
                            MediaItem.Builder()
                                .setMediaId(songId)
                                .setMediaMetadata(metadata)
                                .setUri(Uri.parse(playUrl))
                                .build()
                        )
                    }
                }
                withContext(Dispatchers.Main) {
                    searchResults = results
                }
            } catch (e: Exception) {
                Log.e("LXN", "Search error", e)
                try {
                    val url = "https://music.163.com/api/search/get/web?s=${searchQuery}&type=1&limit=20"
                    val request = Request.Builder().url(url).build()
                    val response = client.newCall(request).execute()
                    val body = response.body?.string()
                    val json = JSONObject(body ?: "{}")
                    val songs = json.optJSONObject("result")?.optJSONArray("songs")
                    val resultsList = mutableListOf<MediaItem>()
                    if (songs != null) {
                        for (i in 0 until songs.length()) {
                            val s = songs.getJSONObject(i)
                            val artist = s.optJSONArray("artists")?.optJSONObject(0)?.optString("name") ?: "Unknown"
                            val songId = s.optString("id")
                            val metadata = MediaMetadata.Builder()
                                .setTitle(s.optString("name"))
                                .setArtist(artist)
                                .build()
                            resultsList.add(
                                MediaItem.Builder()
                                    .setMediaId(songId)
                                    .setMediaMetadata(metadata)
                                    .setUri(Uri.parse("https://music.163.com/song/media/outer/url?id=$songId.mp3"))
                                    .build()
                            )
                        }
                    }
                    withContext(Dispatchers.Main) { searchResults = resultsList }
                } catch (e2: Exception) { e2.printStackTrace() }
            } finally {
                isSearching = false
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "SpringMusic", 
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    ) 
                }
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("搜索在线音乐或歌手") },
                    leadingIcon = { Icon(Icons.Default.MusicNote, null) },
                    trailingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                if (isSearching) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    if (searchResults.isNotEmpty()) {
                        item {
                            Text(
                                "搜索结果", 
                                modifier = Modifier.padding(16.dp), 
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        items(searchResults) { mediaItem ->
                            MusicListItem(mediaItem) {
                                val playApiUrl = mediaItem.localConfiguration?.uri?.toString() ?: ""
                                if (playApiUrl.contains("kuwo.cn")) {
                                    val client = OkHttpClient()
                                    val scope = CoroutineScope(Dispatchers.IO)
                                    scope.launch {
                                        try {
                                            val resp = client.newCall(Request.Builder().url(playApiUrl).build()).execute().body?.string()
                                            if (!resp.isNullOrBlank() && resp.startsWith("http")) {
                                                withContext(Dispatchers.Main) {
                                                    controller?.let {
                                                        it.stop()
                                                        it.clearMediaItems()
                                                        it.setMediaItem(mediaItem.buildUpon().setUri(Uri.parse(resp)).build())
                                                        it.prepare()
                                                        it.play()
                                                    }
                                                }
                                            }
                                        } catch (e: Exception) {
                                            Log.e("LXN", "Failed to fetch Kuwo URL", e)
                                        }
                                    }
                                } else {
                                    controller?.let {
                                        it.stop()
                                        it.clearMediaItems()
                                        it.setMediaItem(mediaItem)
                                        it.prepare()
                                        it.play()
                                    }
                                }
                            }
                        }
                        item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
                    }

                    if (hasPermission) {
                        val repository = MediaRepository(context)
                        val musicList = repository.getAllMusic()
                        
                        item {
                            Text(
                                "本地音乐库", 
                                modifier = Modifier.padding(16.dp), 
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        
                        items(musicList) { mediaItem ->
                            MusicListItem(mediaItem) {
                                controller?.let {
                                    it.stop()
                                    it.clearMediaItems()
                                    it.setMediaItem(mediaItem)
                                    it.prepare()
                                    it.play()
                                }
                            }
                        }
                    } else {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Button(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                                data = Uri.parse("package:${context.packageName}")
                                            }
                                            manageFilesLauncher.launch(intent)
                                        } else {
                                            launcher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("开启文件访问权限以加载本地音乐")
                                }
                            }
                        }
                    }
                }
            }
        }
        
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            PlaybackControls(controller)
        }
    }
}

@Composable
fun MusicListItem(item: MediaItem, onClick: () -> Unit) {
    ListItem(
        headlineContent = { 
            Text(
                item.mediaMetadata.title?.toString() ?: "Unknown",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            ) 
        },
        supportingContent = { 
            Text(
                item.mediaMetadata.artist?.toString() ?: "Unknown",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ) 
        },
        leadingContent = {
            Surface(
                tonalElevation = 4.dp,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        },
        modifier = Modifier.clickable { onClick() }.padding(horizontal = 4.dp),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
fun PlaybackControls(controller: MediaController?) {
    var isPlaying by remember { mutableStateOf(controller?.isPlaying ?: false) }
    var currentTitle by remember { mutableStateOf(controller?.currentMediaItem?.mediaMetadata?.title?.toString() ?: "未在播放") }
    var currentArtist by remember { mutableStateOf(controller?.currentMediaItem?.mediaMetadata?.artist?.toString() ?: "SpringMusic") }

    DisposableEffect(controller) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentTitle = mediaItem?.mediaMetadata?.title?.toString() ?: "未在播放"
                currentArtist = mediaItem?.mediaMetadata?.artist?.toString() ?: "SpringMusic"
            }
        }
        controller?.addListener(listener)
        isPlaying = controller?.isPlaying ?: false
        currentTitle = controller?.currentMediaItem?.mediaMetadata?.title?.toString() ?: "未在播放"
        currentArtist = controller?.currentMediaItem?.mediaMetadata?.artist?.toString() ?: "SpringMusic"
        onDispose {
            controller?.removeListener(listener)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .height(72.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    currentTitle, 
                    style = MaterialTheme.typography.bodyLarge, 
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    currentArtist, 
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { controller?.seekToPrevious() }) {
                    Icon(Icons.Default.SkipPrevious, "Previous", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                
                FilledIconButton(
                    onClick = { if (isPlaying) controller?.pause() else controller?.play() },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        contentColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play"
                    )
                }

                IconButton(onClick = { controller?.seekToNext() }) {
                    Icon(Icons.Default.SkipNext, "Next", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
    }
}
