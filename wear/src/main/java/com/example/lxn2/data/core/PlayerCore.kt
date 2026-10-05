@file:Suppress("unused")

package com.example.lxn2.data.core

import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DislikeInfo(
    val titles: Set<String> = emptySet(),
    val singers: Set<String> = emptySet(),
    val compositeKeys: Set<String> = emptySet(),
)

/**
 * Core player logic engine mapped directly from playInfo, playStatus, progress,
 * tempPlayList, timeoutExit, utils, playedList, and player TypeScript modules.
 */
object PlayerCore {

    private var isTempPlay = false

    // --- Temp Play List (稍后播放列表) ---
    private val _tempPlayList = MutableStateFlow<List<MediaItem>>(emptyList())
    val tempPlayList: StateFlow<List<MediaItem>> = _tempPlayList.asStateFlow()

    // --- Played List (已播放歌曲历史 - 当前循环) ---
    private val _playedList = MutableStateFlow<List<MediaItem>>(emptyList())
    val playedList: StateFlow<List<MediaItem>> = _playedList.asStateFlow()

    // --- Dislike Info (不喜欢信息) ---
    private val _dislikeInfo = MutableStateFlow(DislikeInfo())

    // --- Timeout Exit Flag (播放完整歌曲后停止) ---
    private val _isPlayedStop = MutableStateFlow(false)
    val isPlayedStop: StateFlow<Boolean> = _isPlayedStop.asStateFlow()

    // --- Core Setter Methods ---
    fun setStatusText(@Suppress("UNUSED_PARAMETER") text: String) {
        // Status message updated
    }

    fun setIsPlayedStop(stop: Boolean) {
        _isPlayedStop.value = stop
    }

    // --- Temp Play List Management (tempPlayList.ts) ---
    fun addTempPlayList(items: List<MediaItem>, controller: MediaController? = null) {
        val current = _tempPlayList.value.toMutableList()
        current.addAll(items)
        _tempPlayList.value = current

        // If no music is currently playing, start playing from temp list immediately
        if (controller?.currentMediaItem == null && items.isNotEmpty()) {
            controller?.let { popAndPlayTemp(it) }
        }
    }

    fun removeTempPlayList(index: Int) {
        val current = _tempPlayList.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _tempPlayList.value = current
        }
    }

    fun clearTempPlayList() {
        _tempPlayList.value = emptyList()
    }

    fun popTempItem(): MediaItem? {
        val current = _tempPlayList.value.toMutableList()
        if (current.isEmpty()) return null
        val item = current.removeAt(0)
        _tempPlayList.value = current
        return item
    }

    private fun popAndPlayTemp(controller: MediaController) {
        val item = popTempItem() ?: return
        isTempPlay = true
        controller.setMediaItem(item)
        controller.prepare()
        controller.play()
    }

    // --- Played List Management (playedList.ts) ---
    fun addPlayedList(item: MediaItem) {
        val current = _playedList.value.toMutableList()
        // Avoid duplicate contiguous entries
        if (current.isEmpty() || current.last().mediaId != item.mediaId) {
            current.add(item)
            _playedList.value = current
        }
    }

    fun removePlayedList(index: Int) {
        val current = _playedList.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _playedList.value = current
        }
    }

    fun clearPlayedList() {
        _playedList.value = emptyList()
    }

    // --- Dislike Management (dislikeList & utils.ts) ---
    fun setDislikeInfo(info: DislikeInfo) {
        _dislikeInfo.value = info
    }

    fun addDislike(title: String, singer: String): DislikeInfo {
        val cleanTitle = title.trim().lowercase()
        val cleanSinger = singer.trim().lowercase()
        val key = "$cleanTitle::$cleanSinger"

        val updated = DislikeInfo(
            titles = _dislikeInfo.value.titles + cleanTitle,
            singers = _dislikeInfo.value.singers + cleanSinger,
            compositeKeys = _dislikeInfo.value.compositeKeys + key,
        )
        _dislikeInfo.value = updated
        return updated
    }

    fun isDisliked(item: MediaItem, info: DislikeInfo = _dislikeInfo.value): Boolean {
        val title = item.mediaMetadata.title?.toString()?.trim()?.lowercase() ?: ""
        val singer = item.mediaMetadata.artist?.toString()?.trim()?.lowercase() ?: ""

        return info.titles.contains(title) ||
            info.singers.contains(singer) ||
            info.compositeKeys.contains("$title::$singer")
    }

    // --- List Filtering (utils.ts) ---
    fun filterMusicList(
        list: List<MediaItem>,
        playedList: List<MediaItem>,
        dislikeInfo: DislikeInfo = _dislikeInfo.value,
    ): FilterResult {
        val playedIds = playedList.asSequence().map { it.mediaId }.toSet()
        val canPlayList = mutableListOf<MediaItem>()

        val filteredList = list.filter { item ->
            val disliked = isDisliked(item, dislikeInfo)
            if (!disliked) {
                canPlayList.add(item)
            }
            !disliked && !playedIds.contains(item.mediaId)
        }

        return FilterResult(
            filteredList = filteredList,
            canPlayList = canPlayList,
        )
    }

    data class FilterResult(
        val filteredList: List<MediaItem>,
        val canPlayList: List<MediaItem>,
    )

    /**
     * Async/Wrapper filter list that clears playedList when all non-disliked items have been played.
     */
    fun filterList(list: List<MediaItem>): List<MediaItem> {
        val result = filterMusicList(list, _playedList.value, _dislikeInfo.value)
        return if (result.filteredList.isEmpty() && _playedList.value.isNotEmpty()) {
            clearPlayedList()
            result.canPlayList
        } else {
            result.filteredList
        }
    }

    // --- Next Resolution (player.ts) ---
    fun resolveNextItem(
        controller: MediaController?,
        currentList: List<MediaItem>,
    ): MediaItem? {
        // Priority 1: Check Temp Play List
        if (_tempPlayList.value.isNotEmpty()) {
            isTempPlay = true
            return popTempItem()
        }

        // Priority 2: Filter List
        val currentItem = controller?.currentMediaItem
        val candidates = filterList(currentList)
        if (candidates.isEmpty()) return null

        val currentIndex = candidates.indexOfFirst { it.mediaId == currentItem?.mediaId }
        val nextIndex = if (currentIndex in candidates.indices && currentIndex < candidates.size - 1) {
            currentIndex + 1
        } else {
            0
        }
        isTempPlay = false
        return candidates[nextIndex]
    }
}
