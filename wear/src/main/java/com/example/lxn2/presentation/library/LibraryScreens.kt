@file:OptIn(ExperimentalHorologistApi::class)
package com.example.lxn2.presentation.library

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.example.lxn2.data.MediaRepository
import com.example.lxn2.data.PlaybackPersistence
import com.example.lxn2.presentation.components.fadingEdge
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.composables.MarqueeText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

@Composable
fun LibraryScreen(onLocalMusicClick: () -> Unit, onArtistsClick: () -> Unit, onAlbumsClick: () -> Unit) {
    val columnState = rememberScalingLazyListState()
    ScreenScaffold(scrollState = columnState) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().fadingEdge(),
            state = columnState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 30.dp)
        ) {
            item { ListHeader { Text("库") } }
            
            // 1. 下载 (Download)
            item {
                FilledTonalButton(
                    onClick = { /* TODO */ },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("下载") },
                    icon = { Icon(Icons.Default.Download, null, modifier = Modifier.size(24.dp)) }
                )
            }

            // 2. 歌单 (Playlist)
            item {
                FilledTonalButton(
                    onClick = onLocalMusicClick,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("歌单") },
                    icon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, null, modifier = Modifier.size(24.dp)) }
                )
            }

            // 3. 艺人 (Artist)
            item {
                FilledTonalButton(
                    onClick = onArtistsClick,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("艺人") },
                    icon = { Icon(Icons.Default.Person, null, modifier = Modifier.size(24.dp)) }
                )
            }

            // 4. 专辑 (Album)
            item {
                FilledTonalButton(
                    onClick = onAlbumsClick,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("专辑") },
                    icon = { Icon(Icons.Default.Album, null, modifier = Modifier.size(24.dp)) }
                )
            }
        }
    }
}

@Composable
fun PlaylistScreen(controller: MediaController?, persistence: PlaybackPersistence, onDismiss: () -> Unit) {
    val columnState = rememberScalingLazyListState()
    var mediaItems by remember { mutableStateOf(emptyList<MediaItem>()) }
    fun updateList() {
        val items = mutableListOf<MediaItem>()
        controller?.let { for (i in 0 until it.mediaItemCount) items.add(it.getMediaItemAt(i)) }
        mediaItems = items
    }
    DisposableEffect(controller) {
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                if (events.containsAny(Player.EVENT_TIMELINE_CHANGED, Player.EVENT_PLAYLIST_METADATA_CHANGED)) updateList()
            }
        }
        controller?.addListener(listener)
        updateList()
        onDispose { controller?.removeListener(listener) }
    }
    val history = remember { persistence.getPlaybackHistory() }
    val currentMediaId = controller?.currentMediaItem?.mediaId
    val sortedItems = remember(mediaItems, history) {
        mediaItems.distinctBy { it.mediaId }.sortedWith { a, b ->
            val indexA = history.indexOf(a.mediaId)
            val indexB = history.indexOf(b.mediaId)
            when {
                indexA != -1 && indexB != -1 -> indexA.compareTo(indexB)
                indexA != -1 -> -1
                indexB != -1 -> 1
                else -> (a.mediaMetadata.title?.toString() ?: "").compareTo(b.mediaMetadata.title?.toString() ?: "")
            }
        }
    }
    ScreenScaffold(scrollState = columnState) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().fadingEdge(),
            state = columnState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 30.dp)
        ) {
            item { ListHeader { Text("播放列表") } }
            if (sortedItems.isEmpty()) {
                item { Text(text = "列表为空，请先扫描音乐", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
            } else {
                items(sortedItems) { item: MediaItem ->
                    val isCurrent = item.mediaId == currentMediaId
                    FilledTonalButton(
                        onClick = {
                            controller?.let {
                                for (i in 0 until it.mediaItemCount) {
                                    if (it.getMediaItemAt(i).mediaId == item.mediaId) { it.seekTo(i, 0L); it.play(); onDismiss(); break }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    MarqueeText(
                                        text = item.mediaMetadata.title?.toString() ?: "未知曲目",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        marqueeDpPerSecond = 10.dp,
                                        edgeGradientWidth = 60.dp
                                    )
                                    MarqueeText(
                                        text = item.mediaMetadata.artist?.toString() ?: "未知歌手",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        marqueeDpPerSecond = 8.dp,
                                        edgeGradientWidth = 45.dp
                                    )
                                }
                                if (isCurrent) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SearchScreen(controller: MediaController?, onDismiss: () -> Unit) {
    val appContext = LocalContext.current
    val columnState = rememberScalingLazyListState()
    val repository = remember { MediaRepository(appContext) }
    
    var keyword by remember { mutableStateOf("") }
    var onlineResults by remember { mutableStateOf(emptyList<MediaItem>()) }
    var localResults by remember { mutableStateOf(emptyList<MediaItem>()) }
    var isSearching by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val res = android.app.RemoteInput.getResultsFromIntent(result.data)
            res?.getCharSequence("search_keyword")?.toString()?.let { keyword = it }
        }
    }

    LaunchedEffect(keyword) {
        if (keyword.isNotEmpty()) {
            isSearching = true
            onlineResults = emptyList()
            
            withContext(Dispatchers.IO) {
                val allLocal = repository.getCachedMusic()
                localResults = allLocal.filter {
                    (it.mediaMetadata.title?.toString()?.contains(keyword, ignoreCase = true) == true) ||
                    (it.mediaMetadata.artist?.toString()?.contains(keyword, ignoreCase = true) == true)
                }
            }

            val client = OkHttpClient()
            withContext(Dispatchers.IO) {
                try {
                    val encodedKeyword = java.net.URLEncoder.encode(keyword, "UTF-8")
                    val searchUrl = "http://search.kuwo.cn/r.s?client=kt&all=$encodedKeyword&pn=0&rn=20&uid=794762570&ver=kwplayer_ar_9.2.2.1&vipver=1&show_copyright_off=1&newver=1&ft=music&encoding=utf8&rformat=json&mobi=1"
                    
                    val request = Request.Builder().url(searchUrl).build()
                    val resp = client.newCall(request).execute().body.string()
                    
                    val cleanJson = resp.replace("'", "\"")
                    val json = JSONObject(cleanJson)
                    val songs = json.optJSONArray("abslist")
                    val list = mutableListOf<MediaItem>()
                    if (songs != null) {
                        for (i in 0 until songs.length()) {
                            val s = songs.getJSONObject(i)
                            val songId = s.optString("MUSICRID").replace("MUSIC_", "")
                            val artist = s.optString("ARTIST")
                            val title = s.optString("SONGNAME")
                            val metadata = MediaMetadata.Builder()
                                .setTitle(title)
                                .setArtist(artist)
                                .setExtras(Bundle().apply { putString("origin", "ONLINE") })
                                .build()
                            
                            val playUrl = "http://antiserver.kuwo.cn/anti.s?format=mp3&rid=MUSIC_$songId&type=convert_url&response=url&bitrate=128"
                            
                            list.add(
                                MediaItem.Builder()
                                    .setMediaId(songId)
                                    .setMediaMetadata(metadata)
                                    .setUri(playUrl.toUri())
                                    .build()
                            )
                        }
                    }
                    withContext(Dispatchers.Main) {
                        onlineResults = list
                    }
                } catch (e: Exception) { 
                    Log.e("LXN", "Search Failed", e) 
                    try {
                        val searchUrl = "https://music.163.com/api/search/get/web?s=$keyword&type=1&limit=20"
                        val request = Request.Builder().url(searchUrl).build()
                        val resp = client.newCall(request).execute().body.string()
                        val json = JSONObject(resp)
                        val songs = json.optJSONObject("result")?.optJSONArray("songs")
                        val list = mutableListOf<MediaItem>()
                        if (songs != null) {
                            for (i in 0 until songs.length()) {
                                val s = songs.getJSONObject(i)
                                val songId = s.optString("id")
                                val artist = s.optJSONArray("artists")?.optJSONObject(0)?.optString("name") ?: "Unknown"
                                val metadata = MediaMetadata.Builder()
                                    .setTitle(s.optString("name"))
                                    .setArtist(artist)
                                    .setExtras(Bundle().apply { putString("origin", "ONLINE") })
                                    .build()
                                list.add(
                                    MediaItem.Builder()
                                        .setMediaId(songId)
                                        .setMediaMetadata(metadata)
                                        .setUri("https://music.163.com/song/media/outer/url?id=$songId.mp3".toUri())
                                        .build()
                                )
                            }
                        }
                        withContext(Dispatchers.Main) { onlineResults = list }
                    } catch (e2: Exception) { Log.e("LXN", "Fallback Search Failed", e2) }
                } finally { 
                    isSearching = false 
                }
            }
        }
    }

    ScreenScaffold(scrollState = columnState) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().fadingEdge(),
            state = columnState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 30.dp)
        ) {
            item { ListHeader { Text("搜索") } }
            item { 
                FilledTonalButton(
                    onClick = {
                        try {
                            val intent = Intent("android.support.wearable.input.action.REMOTE_INPUT")
                            val remoteInput = android.app.RemoteInput.Builder("search_keyword")
                                .setLabel("输入歌曲或歌手")
                                .build()
                            intent.putExtra("android.support.wearable.input.extra.REMOTE_INPUTS", arrayOf(remoteInput))
                            launcher.launch(intent)
                        } catch (e: Exception) {
                            Log.e("LXN", "Failed to launch remote input", e)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(keyword.ifEmpty { "点击输入" }) },
                    icon = { Icon(Icons.Default.Keyboard, null) }
                ) 
            }
            if (isSearching) {
                item { 
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator() 
                    }
                }
            } else if (keyword.isNotEmpty() && localResults.isEmpty() && onlineResults.isEmpty()) {
                item {
                    Text(
                        text = "未找到相关歌曲",
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (localResults.isNotEmpty()) {
                item { ListHeader { Text("本地歌曲") } }
                items(localResults) { song: MediaItem ->
                    FilledTonalButton(
                        onClick = {
                            controller?.let {
                                var foundIndex = -1
                                for (i in 0 until it.mediaItemCount) {
                                    if (it.getMediaItemAt(i).mediaId == song.mediaId) { foundIndex = i; break }
                                }
                                if (foundIndex != -1) {
                                    it.seekTo(foundIndex, 0L)
                                } else {
                                    it.addMediaItem(song)
                                    it.seekTo(it.mediaItemCount - 1, 0L)
                                }
                                it.prepare()
                                it.play()
                            }
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Column {
                                Text(song.mediaMetadata.title.toString(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.mediaMetadata.artist.toString(), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

            if (onlineResults.isNotEmpty()) {
                item { ListHeader { Text("在线歌曲") } }
                items(onlineResults) { song: MediaItem ->
                    FilledTonalButton(
                        onClick = {
                            val playApiUrl = song.localConfiguration?.uri?.toString() ?: ""
                            if (playApiUrl.contains("kuwo.cn")) {
                                val client = OkHttpClient()
                                val scope = CoroutineScope(Dispatchers.IO)
                                scope.launch {
                                    try {
                                        val resp = client.newCall(Request.Builder().url(playApiUrl).build()).execute().body.string()
                                        if (resp.startsWith("http")) {
                                            withContext(Dispatchers.Main) {
                                                controller?.let {
                                                    val finalSong = song.buildUpon().setUri(resp.toUri()).build()
                                                    var foundIndex = -1
                                                    for (i in 0 until it.mediaItemCount) {
                                                        if (it.getMediaItemAt(i).mediaId == song.mediaId) { foundIndex = i; break }
                                                    }
                                                    if (foundIndex != -1) {
                                                        it.seekTo(foundIndex, 0L)
                                                    } else {
                                                        it.addMediaItem(finalSong)
                                                        it.seekTo(it.mediaItemCount - 1, 0L)
                                                    }
                                                    it.prepare()
                                                    it.play()
                                                }
                                                onDismiss()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("LXN", "Failed to fetch Kuwo URL", e)
                                    }
                                }
                            } else {
                                controller?.let {
                                    var foundIndex = -1
                                    for (i in 0 until it.mediaItemCount) {
                                        if (it.getMediaItemAt(i).mediaId == song.mediaId) { foundIndex = i; break }
                                    }
                                    if (foundIndex != -1) {
                                        it.seekTo(foundIndex, 0L)
                                    } else {
                                        it.addMediaItem(song)
                                        it.seekTo(it.mediaItemCount - 1, 0L)
                                    }
                                    it.prepare()
                                    it.play()
                                }
                                onDismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Column {
                                Text(song.mediaMetadata.title.toString(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.mediaMetadata.artist.toString(), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ArtistsScreen(onArtistClick: (String) -> Unit) {
    val appContext = LocalContext.current
    val repository = remember { MediaRepository(appContext) }
    val artists = remember { repository.getArtists() }
    val columnState = rememberScalingLazyListState()

    ScreenScaffold(scrollState = columnState) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().fadingEdge(),
            state = columnState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 30.dp)
        ) {
            item { ListHeader { Text("艺人") } }
            if (artists.isEmpty()) {
                item { Text("暂无艺人信息", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
            } else {
                items(artists) { artist ->
                    FilledTonalButton(
                        onClick = { onArtistClick(artist) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(artist, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }
            }
        }
    }
}

@Composable
fun AlbumsScreen(onAlbumClick: (String) -> Unit) {
    val appContext = LocalContext.current
    val repository = remember { MediaRepository(appContext) }
    val albums = remember { repository.getAlbums() }
    val columnState = rememberScalingLazyListState()

    ScreenScaffold(scrollState = columnState) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().fadingEdge(),
            state = columnState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 30.dp)
        ) {
            item { ListHeader { Text("专辑") } }
            if (albums.isEmpty()) {
                item { Text("暂无专辑信息", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
            } else {
                items(albums) { album ->
                    FilledTonalButton(
                        onClick = { onAlbumClick(album) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(album, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun FilteredMusicScreen(title: String, filterType: String, filterValue: String, controller: MediaController?, onDismiss: () -> Unit) {
    val appContext = LocalContext.current
    val repository = remember { MediaRepository(appContext) }
    val musicItems = remember {
        if (filterType == "artist") repository.getMusicByArtist(filterValue)
        else repository.getMusicByAlbum(filterValue)
    }
    val columnState = rememberScalingLazyListState()
    val currentMediaId = controller?.currentMediaItem?.mediaId

    ScreenScaffold(scrollState = columnState) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().fadingEdge(),
            state = columnState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 30.dp)
        ) {
            item { ListHeader { Text(title) } }
            items(musicItems) { item ->
                val isCurrent = item.mediaId == currentMediaId
                FilledTonalButton(
                    onClick = {
                        controller?.let {
                            var foundIndex = -1
                            for (i in 0 until it.mediaItemCount) {
                                if (it.getMediaItemAt(i).mediaId == item.mediaId) { foundIndex = i; break }
                            }
                            if (foundIndex != -1) {
                                it.seekTo(foundIndex, 0L)
                            } else {
                                it.addMediaItem(item)
                                it.seekTo(it.mediaItemCount - 1, 0L)
                            }
                            it.prepare()
                            it.play()
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                MarqueeText(
                                    text = item.mediaMetadata.title?.toString() ?: "未知曲目",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    marqueeDpPerSecond = 10.dp,
                                    edgeGradientWidth = 60.dp
                                )
                            }
                            if (isCurrent) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}
