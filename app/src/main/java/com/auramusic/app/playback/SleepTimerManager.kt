package com.auramusic.app.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SleepTimerState(
    val remainingSeconds: Long? = null,
    val isEndOfTrack: Boolean = false
)

object SleepTimerManager {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var timerJob: Job? = null
    private var onTimerFinished: (() -> Unit)? = null

    private val _timerState = MutableStateFlow(SleepTimerState())
    val timerState: StateFlow<SleepTimerState> = _timerState.asStateFlow()

    fun startTimer(minutes: Int, onFinish: () -> Unit) {
        cancelTimer()
        onTimerFinished = onFinish
        val totalSeconds = minutes * 60L
        _timerState.value = SleepTimerState(remainingSeconds = totalSeconds, isEndOfTrack = false)

        timerJob = scope.launch {
            var currentSec = totalSeconds
            while (currentSec > 0) {
                delay(1000)
                currentSec--
                _timerState.value = SleepTimerState(remainingSeconds = currentSec, isEndOfTrack = false)
            }
            _timerState.value = SleepTimerState(remainingSeconds = null, isEndOfTrack = false)
            onTimerFinished?.invoke()
            onTimerFinished = null
        }
    }

    fun setEndOfTrack(onFinish: () -> Unit) {
        cancelTimer()
        onTimerFinished = onFinish
        _timerState.value = SleepTimerState(remainingSeconds = null, isEndOfTrack = true)
    }

    fun notifyTrackEnded() {
        if (_timerState.value.isEndOfTrack) {
            cancelTimer()
            onTimerFinished?.invoke()
            onTimerFinished = null
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        onTimerFinished = null
        _timerState.value = SleepTimerState(remainingSeconds = null, isEndOfTrack = false)
    }
}
