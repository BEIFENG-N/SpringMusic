package com.example.lxn2.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.media.repository.PlayerRepository
import com.google.android.horologist.media.ui.state.PlayerViewModel

@OptIn(ExperimentalHorologistApi::class)
class PlayerViewModelFactory(
    private val playerRepository: PlayerRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PlayerViewModel(playerRepository) as T
    }
}
