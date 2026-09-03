package com.example.recorder

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class AudioPlayerManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var updateJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPlayingPath = MutableStateFlow<String?>(null)
    val currentPlayingPath: StateFlow<String?> = _currentPlayingPath.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0)
    val currentPositionMs: StateFlow<Int> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0)
    val durationMs: StateFlow<Int> = _durationMs.asStateFlow()

    fun playAudio(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) {
            Log.e("AudioPlayerManager", "File not found: $filePath")
            return
        }

        if (_currentPlayingPath.value == filePath && mediaPlayer != null) {
            if (_isPlaying.value) {
                pause()
            } else {
                mediaPlayer?.start()
                _isPlaying.value = true
                startPositionTracker()
            }
            return
        }

        stop()

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                _durationMs.value = duration
                _currentPositionMs.value = 0
                _currentPlayingPath.value = filePath
                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentPositionMs.value = 0
                    updateJob?.cancel()
                }
                start()
            }
            _isPlaying.value = true
            startPositionTracker()
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Failed to play audio", e)
            stop()
        }
    }

    fun pause() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    _isPlaying.value = false
                    updateJob?.cancel()
                }
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error pausing playback", e)
        }
    }

    fun seekTo(positionMs: Int) {
        try {
            mediaPlayer?.seekTo(positionMs)
            _currentPositionMs.value = positionMs
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error seeking", e)
        }
    }

    fun stop() {
        updateJob?.cancel()
        updateJob = null
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                reset()
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error stopping player", e)
        } finally {
            mediaPlayer = null
            _isPlaying.value = false
            _currentPositionMs.value = 0
            _durationMs.value = 0
            _currentPlayingPath.value = null
        }
    }

    private fun startPositionTracker() {
        updateJob?.cancel()
        updateJob = scope.launch {
            while (isActive && _isPlaying.value) {
                mediaPlayer?.let { player ->
                    try {
                        if (player.isPlaying) {
                            _currentPositionMs.value = player.currentPosition
                        }
                    } catch (e: Exception) {
                        // ignore
                    }
                }
                delay(200)
            }
        }
    }
}
