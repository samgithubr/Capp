package com.example.recorder

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class RecordingState {
    IDLE,
    RECORDING,
    PAUSED,
    STOPPED
}

class AudioRecorderManager(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var recordingStartTime: Long = 0L
    private var pausedDuration: Long = 0L
    private var pauseStartTime: Long = 0L

    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _recordingDurationSeconds = MutableStateFlow(0)
    val recordingDurationSeconds: StateFlow<Int> = _recordingDurationSeconds.asStateFlow()

    private val _currentAmplitude = MutableStateFlow(0f)
    val currentAmplitude: StateFlow<Float> = _currentAmplitude.asStateFlow()

    private var amplitudeJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private fun getRecordingsDir(): File {
        val dir = File(context.filesDir, "recordings")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun startRecording(shopName: String = "CoffeeShop"): String? {
        try {
            stopRecording() // ensure any previous recorder is cleaned up

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val sanitizedShop = shopName.replace(Regex("[^a-zA-Z0-9]"), "_").take(15)
            val fileName = "order_${sanitizedShop}_$timestamp.m4a"
            val outputFile = File(getRecordingsDir(), fileName)
            currentOutputFile = outputFile

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            recordingStartTime = System.currentTimeMillis()
            pausedDuration = 0L
            _recordingState.value = RecordingState.RECORDING
            startAmplitudePolling()
            return outputFile.absolutePath
        } catch (e: Exception) {
            Log.e("AudioRecorderManager", "Error starting recording", e)
            _recordingState.value = RecordingState.IDLE
            return null
        }
    }

    fun pauseRecording() {
        if (_recordingState.value == RecordingState.RECORDING) {
            try {
                mediaRecorder?.pause()
                pauseStartTime = System.currentTimeMillis()
                _recordingState.value = RecordingState.PAUSED
            } catch (e: Exception) {
                Log.e("AudioRecorderManager", "Error pausing recording", e)
            }
        }
    }

    fun resumeRecording() {
        if (_recordingState.value == RecordingState.PAUSED) {
            try {
                mediaRecorder?.resume()
                pausedDuration += System.currentTimeMillis() - pauseStartTime
                _recordingState.value = RecordingState.RECORDING
            } catch (e: Exception) {
                Log.e("AudioRecorderManager", "Error resuming recording", e)
            }
        }
    }

    fun stopRecording(): RecordingResult? {
        if (_recordingState.value == RecordingState.IDLE) return null

        amplitudeJob?.cancel()
        amplitudeJob = null
        _currentAmplitude.value = 0f

        val duration = _recordingDurationSeconds.value
        val file = currentOutputFile

        try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (e: Exception) {
                    Log.w("AudioRecorderManager", "Error in recorder.stop()", e)
                }
                reset()
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioRecorderManager", "Error stopping recorder", e)
        } finally {
            mediaRecorder = null
            _recordingState.value = RecordingState.IDLE
            _recordingDurationSeconds.value = 0
        }

        return if (file != null && file.exists()) {
            RecordingResult(filePath = file.absolutePath, durationSeconds = duration, fileName = file.name)
        } else {
            null
        }
    }

    private fun startAmplitudePolling() {
        amplitudeJob?.cancel()
        amplitudeJob = scope.launch {
            while (isActive && _recordingState.value != RecordingState.IDLE) {
                if (_recordingState.value == RecordingState.RECORDING) {
                    val currentMillis = System.currentTimeMillis() - recordingStartTime - pausedDuration
                    _recordingDurationSeconds.value = (currentMillis / 1000).toInt()

                    val maxAmp = try {
                        mediaRecorder?.maxAmplitude ?: 0
                    } catch (e: Exception) {
                        0
                    }
                    val normalized = (maxAmp / 32767f).coerceIn(0f, 1f)
                    _currentAmplitude.value = normalized
                }
                delay(100)
            }
        }
    }
}

data class RecordingResult(
    val filePath: String,
    val durationSeconds: Int,
    val fileName: String
)
