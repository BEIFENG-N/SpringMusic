package com.example.lxn2.data

import android.net.Uri

data class MusicItem(
    val id: String,
    val title: String,
    val artist: String,
    val mediaUri: Uri,
    val isLocal: Boolean
)
