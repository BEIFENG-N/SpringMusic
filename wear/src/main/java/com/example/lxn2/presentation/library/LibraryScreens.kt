@file:OptIn(ExperimentalHorologistApi::class, ExperimentalFoundationApi::class)
package com.example.lxn2.presentation.library

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.lxn2.R
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
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material3.Text
import com.example.lxn2.LXNApplication
import com.example.lxn2.data.AppConstants
import com.example.lxn2.data.MediaRepository
import com.example.lxn2.data.PlaybackPersistence
import com.example.lxn2.data.source.MusicSourceEngine
import com.example.lxn2.presentation.components.fadingEdge
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.composables.MarqueeText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

@Composable
fun LibraryScreen(
    onLocalMusicClick: () -> Unit,
    onTempPlayListClick: () -> Unit,
    onArtistsClick: () -> Unit,
    onAlbumsClick: () -> Unit
) {
    val columnState = rememberScalingLazyListState()
    ScreenScaffold(
        scrollState = columnState,
        scrollIndicator = { ScrollIndicator(columnState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().fadingEdge(),
            state = columnState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 30.dp)
        ) {
            item { ListHeader { Text(stringResource(R.string.library_title)) } }
            
            // 1. 稍后播放
            item {
                FilledTonalButton(
                    onClick = onTempPlayListClick,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.library_temp_playlist)) },
                    icon = { Icon(Icons.Default.Schedule, null, modifier = Modifier.size(24.dp)) }
                )
            }

            // 2. 歌单
            item {
                FilledTonalButton(
                    onClick = onLocalMusicClick,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.library_playlist)) },
                    icon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, null, modifier = Modifier.size(24.dp)) }
                )
            }

            // 3. 艺人
            item {
                FilledTonalButton(
                    onClick = onArtistsClick,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.library_artists)) },
                    icon = { Icon(Icons.Default.Person, null, modifier = Modifier.size(24.dp)) }
                )
            }

            // 4. 专辑
            item {
                FilledTonalButton(
                    onClick = onAlbumsClick,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.library_albums)) },
                    icon = { Icon(Icons.Default.Album, null, modifier = Modifier.size(24.dp)) }
                )
            }
        }
    }
}

@Composable
fun SongListItem(
    item: MediaItem,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val unknownTrack = stringResource(R.string.unknown_track)
                val unknownArtist = stringResource(R.string.unknown_artist)
                if (isCurrent) {
                    MarqueeText(
                        text = item.mediaMetadata.title?.toString() ?: unknownTrack,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        marqueeDpPerSecond = 10.dp,
                        edgeGradientWidth = 60.dp
                    )
                    MarqueeText(
                        text = item.mediaMetadata.artist?.toString() ?: unknownArtist,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        marqueeDpPerSecond = 8.dp,
                        edgeGradientWidth = 45.dp
                    )
                } else {
                    Text(
                        text = item.mediaMetadata.title?.toString() ?: unknownTrack,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.mediaMetadata.artist?.toString() ?: unknownArtist,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (isCurrent) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun SongActionDialog(
    mediaItem: MediaItem,
    persistence: PlaybackPersistence,
    controller: MediaController?,
    onPlayNow: () -> Unit,
    onDismiss: () -> Unit
) {
    val columnState = rememberScalingLazyListState()
    var isFav by remember { mutableStateOf(persistence.isFavorite(mediaItem.mediaId)) }

    ScreenScaffold(
        scrollState = columnState,
        scrollIndicator = { ScrollIndicator(columnState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().fadingEdge(),
            state = columnState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 24.dp)
        ) {
            item {
                ListHeader {
                    Text(
                        text = mediaItem.mediaMetadata.title?.toString() ?: "曲目选项",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            item {
                FilledTonalButton(
                    onClick = {
                        onPlayNow()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("立即播放") },
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                )
            }
            item {
                FilledTonalButton(
                    onClick = {
                        com.example.lxn2.data.core.PlayerCore.addTempPlayList(listOf(mediaItem), controller)
                        com.example.lxn2.data.core.PlayerCore.setStatusText("已添加到稍后播放")
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("稍后播放") },
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                )
            }
            item {
                FilledTonalButton(
                    onClick = {
                        isFav = persistence.toggleFavorite(mediaItem.mediaId)
                        val msg = if (isFav) "已收藏" else "已取消收藏"
                        com.example.lxn2.data.core.PlayerCore.setStatusText(msg)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (isFav) "取消收藏" else "收藏歌曲") },
                    icon = {
                        Icon(
                            imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isFav) Color.Red else MaterialTheme.colorScheme.primary
                        )
                    }
                )
            }
            item {
                FilledTonalButton(
                    onClick = {
                        val title = mediaItem.mediaMetadata.title?.toString() ?: ""
                        val singer = mediaItem.mediaMetadata.artist?.toString() ?: ""
                        val updated = com.example.lxn2.data.core.PlayerCore.addDislike(title, singer)
                        persistence.saveDislikeKeys(updated.compositeKeys)
                        com.example.lxn2.data.core.PlayerCore.setStatusText("已加入不喜欢")
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("加入不喜欢") },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.ThumbDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                )
            }
            item {
                FilledTonalButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    label = { Text("关闭") },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun PlaylistScreen(controller: MediaController?, persistence: PlaybackPersistence, onDismiss: () -> Unit) {
    var actionSong by remember { mutableStateOf<MediaItem?>(null) }
    if (actionSong != null) {
        SongActionDialog(
            mediaItem = actionSong!!,
            persistence = persistence,
            controller = controller,
            onPlayNow = {
                controller?.let {
                    for (i in 0 until it.mediaItemCount) {
                        if (it.getMediaItemAt(i).mediaId == actionSong!!.mediaId) {
                            it.seekTo(i, 0L)
                            it.play()
                            onDismiss()
                            break
                        }
                    }
                }
            },
            onDismiss = { actionSong = null }
        )
    } else {
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
        ScreenScaffold(
            scrollState = columnState,
            scrollIndicator = { ScrollIndicator(columnState) }
        ) {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize().fadingEdge(),
                state = columnState,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 30.dp)
            ) {
                item { ListHeader { Text(stringResource(R.string.library_playlist)) } }
                if (sortedItems.isEmpty()) {
                    item { Text(text = stringResource(R.string.playlist_empty_hint), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
                } else {
                    items(sortedItems) { item: MediaItem ->
                        val isCurrent = item.mediaId == currentMediaId
                        SongListItem(
                            item = item,
                            isCurrent = isCurrent,
                            onClick = {
                                controller?.let {
                                    for (i in 0 until it.mediaItemCount) {
                                        if (it.getMediaItemAt(i).mediaId == item.mediaId) { it.seekTo(i, 0L); it.play(); onDismiss(); break }
                                    }
                                }
                            },
                            onLongClick = {
                                actionSong = item
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchScreen(
    controller: MediaController?,
    persistence: PlaybackPersistence,
    onDismiss: () -> Unit
) {
    var actionSong by remember { mutableStateOf<MediaItem?>(null) }
    if (actionSong != null) {
        SongActionDialog(
            mediaItem = actionSong!!,
            persistence = persistence,
            controller = controller,
            onPlayNow = {
                controller?.let {
                    var foundIndex = -1
                    for (i in 0 until it.mediaItemCount) {
                        if (it.getMediaItemAt(i).mediaId == actionSong!!.mediaId) { foundIndex = i; break }
                    }
                    if (foundIndex != -1) {
                        it.seekTo(foundIndex, 0L)
                    } else {
                        it.addMediaItem(actionSong!!)
                        it.seekTo(it.mediaItemCount - 1, 0L)
                    }
                    it.prepare()
                    it.play()
                    onDismiss()
                }
            },
            onDismiss = { actionSong = null }
        )
    } else {
        val appContext = LocalContext.current
        val columnState = rememberScalingLazyListState()
        val repository = remember { (appContext.applicationContext as LXNApplication).container.mediaRepository }
        
        var keyword by remember { mutableStateOf("") }
        var currentPage by remember(keyword) { mutableIntStateOf(1) }
        var onlineResults by remember { mutableStateOf(emptyList<MediaItem>()) }
        var localResults by remember { mutableStateOf(emptyList<MediaItem>()) }
        var isSearching by remember { mutableStateOf(false) }
        var historyList by remember { mutableStateOf(persistence.getSearchHistory()) }

        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val res = android.app.RemoteInput.getResultsFromIntent(result.data)
                res?.getCharSequence("search_keyword")?.toString()?.let { keyword = it }
            }
        }

        LaunchedEffect(keyword, currentPage) {
            if (keyword.isNotEmpty()) {
                // 500ms 搜索防抖节流
                delay(500)

                persistence.addSearchHistory(keyword)
                historyList = persistence.getSearchHistory()
                isSearching = true

                if (currentPage == 1) {
                    onlineResults = emptyList()
                    withContext(Dispatchers.IO) {
                        val allLocal = repository.getCachedMusic()
                        localResults = allLocal.filter {
                            (it.mediaMetadata.title?.toString()?.contains(keyword, ignoreCase = true) == true) ||
                            (it.mediaMetadata.artist?.toString()?.contains(keyword, ignoreCase = true) == true)
                        }
                    }
                }

                withContext(Dispatchers.IO) {
                    try {
                        val searchResultJson = MusicSourceEngine.searchMusic(keyword, page = currentPage)
                        
                        val jsonArray = JSONArray(searchResultJson)
                        val list = mutableListOf<MediaItem>()
                        
                        for (i in 0 until jsonArray.length()) {
                            val s = jsonArray.getJSONObject(i)
                            val songId = s.optString("id")
                            val source = s.optString("source")
                            val metadata = MediaMetadata.Builder()
                                .setTitle(s.optString("title"))
                                .setArtist(s.optString("artist"))
                                .setExtras(Bundle().apply { 
                                    putString("origin", "ONLINE")
                                    putString("source", source)
                                })
                                .build()
                                
                            list.add(
                                MediaItem.Builder()
                                    .setMediaId(songId)
                                    .setMediaMetadata(metadata)
                                    .setUri("lxmusic://online?id=$songId&source=$source".toUri()) 
                                    .build()
                            )
                        }
                        withContext(Dispatchers.Main) {
                            onlineResults = if (currentPage == 1) list else onlineResults + list
                        }
                    } catch (e: Exception) { 
                        Log.e(AppConstants.LOG_TAG, "Search Failed via JS Engine", e) 
                    } finally { 
                        isSearching = false 
                    }
                }
            }
        }

        ScreenScaffold(
            scrollState = columnState,
            scrollIndicator = { ScrollIndicator(columnState) }
        ) {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize().fadingEdge(),
                state = columnState,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 30.dp)
            ) {
                item { ListHeader { Text(stringResource(R.string.search_title)) } }
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
                if (keyword.isEmpty() && historyList.isNotEmpty()) {
                    item { ListHeader { Text("搜索历史") } }
                    items(historyList) { hist ->
                        FilledTonalButton(
                            onClick = { keyword = hist },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            label = { Text(hist, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        )
                    }
                    item {
                        FilledTonalButton(
                            onClick = {
                                persistence.clearSearchHistory()
                                historyList = emptyList()
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            label = { Text("清空搜索历史") }
                        )
                    }
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
                        val isCurrent = song.mediaId == controller?.currentMediaItem?.mediaId
                        SongListItem(
                            item = song,
                            isCurrent = isCurrent,
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
                            onLongClick = {
                                actionSong = song
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }

                if (onlineResults.isNotEmpty()) {
                    item { ListHeader { Text("在线歌曲") } }
                    items(onlineResults) { song: MediaItem ->
                        val isCurrent = song.mediaId == controller?.currentMediaItem?.mediaId
                        SongListItem(
                            item = song,
                            isCurrent = isCurrent,
                            onClick = {
                                val scope = CoroutineScope(Dispatchers.IO)
                                scope.launch {
                                    try {
                                        val source = song.mediaMetadata.extras?.getString("source") ?: ""
                                        val realUrl = MusicSourceEngine.getMusicUrl(song.mediaId, source)
                                        
                                        if (realUrl.isNotBlank()) {
                                            withContext(Dispatchers.Main) {
                                                controller?.let {
                                                    val finalSong = song.buildUpon().setUri(realUrl.toUri()).build()
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
                                        Log.e(AppConstants.LOG_TAG, "Failed to fetch real URL via JS", e)
                                    }
                                }
                            },
                            onLongClick = {
                                actionSong = song
                            }
                        )
                    }
                    if (!isSearching) {
                        item {
                            FilledTonalButton(
                                onClick = { currentPage++ },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                label = { Text("加载下一页 (页码: ${currentPage + 1})") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArtistsScreen(onArtistClick: (String) -> Unit) {
    val appContext = LocalContext.current
    val repository = remember { (appContext.applicationContext as LXNApplication).container.mediaRepository }
    val artists = remember { repository.getArtists() }
    val columnState = rememberScalingLazyListState()

    ScreenScaffold(
        scrollState = columnState,
        scrollIndicator = { ScrollIndicator(columnState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().fadingEdge(),
            state = columnState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 30.dp)
        ) {
            item { ListHeader { Text(stringResource(R.string.library_artists)) } }
            if (artists.isEmpty()) {
                item { Text(stringResource(R.string.no_artists), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
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
    val repository = remember { (appContext.applicationContext as LXNApplication).container.mediaRepository }
    val albums = remember { repository.getAlbums() }
    val columnState = rememberScalingLazyListState()

    ScreenScaffold(
        scrollState = columnState,
        scrollIndicator = { ScrollIndicator(columnState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().fadingEdge(),
            state = columnState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 30.dp)
        ) {
            item { ListHeader { Text(stringResource(R.string.library_albums)) } }
            if (albums.isEmpty()) {
                item { Text(stringResource(R.string.no_albums), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
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
fun FilteredMusicScreen(
    title: String,
    filterType: String,
    filterValue: String,
    controller: MediaController?,
    persistence: PlaybackPersistence,
    onDismiss: () -> Unit
) {
    var actionSong by remember { mutableStateOf<MediaItem?>(null) }
    if (actionSong != null) {
        SongActionDialog(
            mediaItem = actionSong!!,
            persistence = persistence,
            controller = controller,
            onPlayNow = {
                controller?.let {
                    var foundIndex = -1
                    for (i in 0 until it.mediaItemCount) {
                        if (it.getMediaItemAt(i).mediaId == actionSong!!.mediaId) { foundIndex = i; break }
                    }
                    if (foundIndex != -1) {
                        it.seekTo(foundIndex, 0L)
                    } else {
                        it.addMediaItem(actionSong!!)
                        it.seekTo(it.mediaItemCount - 1, 0L)
                    }
                    it.prepare()
                    it.play()
                    onDismiss()
                }
            },
            onDismiss = { actionSong = null }
        )
    } else {
        val appContext = LocalContext.current
        val repository = remember { (appContext.applicationContext as LXNApplication).container.mediaRepository }
        val musicItems = remember {
            if (filterType == "artist") repository.getMusicByArtist(filterValue)
            else repository.getMusicByAlbum(filterValue)
        }
        val columnState = rememberScalingLazyListState()
        val currentMediaId = controller?.currentMediaItem?.mediaId

        ScreenScaffold(
            scrollState = columnState,
            scrollIndicator = { ScrollIndicator(columnState) }
        ) {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize().fadingEdge(),
                state = columnState,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 30.dp)
            ) {
                item { ListHeader { Text(title) } }
                items(musicItems) { item ->
                    val isCurrent = item.mediaId == currentMediaId
                    SongListItem(
                        item = item,
                        isCurrent = isCurrent,
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
                        onLongClick = {
                            actionSong = item
                        }
                    )
                }
            }
        }
    }
}
