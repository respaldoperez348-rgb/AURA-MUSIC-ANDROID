package com.auramusic.app.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber

enum class EqPreset(val displayName: String, val bandMillibels: List<Short>) {
    FLAT("Plano", listOf(0, 0, 0, 0, 0)),
    STUDIO("Estudio AURA", listOf(250, 150, 0, 200, 350)),
    BASS("Graves+", listOf(550, 350, 0, -50, 100)),
    VOCAL("Voces", listOf(-150, 100, 450, 350, 100)),
    ROCK("Rock", listOf(400, 150, -100, 250, 450)),
    ELECTRONIC("Electrónica", listOf(450, 250, 50, 200, 400)),
}

data class AudioLabState(
    val eqEnabled: Boolean = false,
    val selectedPreset: EqPreset = EqPreset.FLAT,
    val bandLevels: List<Short> = listOf(0, 0, 0, 0, 0),
    val bandFreqLabels: List<String> = listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz"),
    val minBandLevel: Short = -1500,
    val maxBandLevel: Short = 1500,
    val bassBoostStrength: Int = 0, // 0..1000
    val reverbPreset: Short = PresetReverb.PRESET_NONE,
    val playbackSpeed: Float = 1.0f,
    val playbackPitch: Float = 1.0f,
    val sleepTimerRemainingMs: Long = 0L,
    val sleepTimerStopAfterSong: Boolean = false,
)

/**
 * Motor DSP nativo de Android (Equalizer + BassBoost + PresetReverb) enlazado al audioSessionId de ExoPlayer.
 */
class AudioLabController {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var presetReverb: PresetReverb? = null
    private var attachedSessionId: Int = 0

    private val _state = MutableStateFlow(AudioLabState())
    val state: StateFlow<AudioLabState> = _state.asStateFlow()

    fun attachAudioSession(audioSessionId: Int) {
        if (audioSessionId <= 0 || audioSessionId == attachedSessionId) return
        releaseEffects()
        attachedSessionId = audioSessionId

        runCatching {
            val eq = Equalizer(0, audioSessionId).apply {
                enabled = _state.value.eqEnabled
            }
            val numBands = eq.numberOfBands.toInt().coerceAtMost(5)
            val range = eq.bandLevelRange
            val minLv = range.getOrNull(0) ?: -1500
            val maxLv = range.getOrNull(1) ?: 1500
            val labels = (0 until numBands).map { idx ->
                val hz = eq.getCenterFreq(idx.toShort()) / 1000
                if (hz >= 1000) "${hz / 1000}kHz" else "${hz}Hz"
            }
            val levels = _state.value.bandLevels.take(numBands).ifEmpty { List(numBands) { 0.toShort() } }
            levels.forEachIndexed { idx, lv ->
                runCatching { eq.setBandLevel(idx.toShort(), lv.coerceIn(minLv, maxLv)) }
            }
            equalizer = eq
            _state.update {
                it.copy(
                    bandFreqLabels = labels.ifEmpty { it.bandFreqLabels },
                    minBandLevel = minLv,
                    maxBandLevel = maxLv,
                    bandLevels = levels
                )
            }
        }.onFailure { Timber.w(it, "Equalizer no soportado en esta sesión") }

        runCatching {
            val bb = BassBoost(0, audioSessionId).apply {
                val str = _state.value.bassBoostStrength.toShort()
                enabled = str > 0
                if (strengthSupported && str > 0) setStrength(str)
            }
            bassBoost = bb
        }.onFailure { Timber.w(it, "BassBoost no soportado en esta sesión") }

        runCatching {
            val rv = PresetReverb(0, audioSessionId).apply {
                val p = _state.value.reverbPreset
                enabled = p != PresetReverb.PRESET_NONE
                if (p != PresetReverb.PRESET_NONE) preset = p
            }
            presetReverb = rv
        }.onFailure { Timber.w(it, "PresetReverb no soportado en esta sesión") }
    }

    fun setEqEnabled(enabled: Boolean) {
        _state.update { it.copy(eqEnabled = enabled) }
        runCatching { equalizer?.enabled = enabled }
    }

    fun applyPreset(preset: EqPreset) {
        val levels = preset.bandMillibels
        _state.update { it.copy(eqEnabled = true, selectedPreset = preset, bandLevels = levels) }
        runCatching {
            equalizer?.let { eq ->
                eq.enabled = true
                val count = eq.numberOfBands.toInt().coerceAtMost(levels.size)
                for (i in 0 until count) {
                    eq.setBandLevel(i.toShort(), levels[i].coerceIn(_state.value.minBandLevel, _state.value.maxBandLevel))
                }
            }
        }
    }

    fun setBandLevel(bandIndex: Int, levelMillibels: Short) {
        val current = _state.value.bandLevels.toMutableList()
        if (bandIndex !in current.indices) return
        val clamped = levelMillibels.coerceIn(_state.value.minBandLevel, _state.value.maxBandLevel)
        current[bandIndex] = clamped
        _state.update { it.copy(eqEnabled = true, bandLevels = current) }
        runCatching {
            equalizer?.let { eq ->
                eq.enabled = true
                eq.setBandLevel(bandIndex.toShort(), clamped)
            }
        }
    }

    fun setBassBoost(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _state.update { it.copy(bassBoostStrength = clamped) }
        runCatching {
            bassBoost?.let { bb ->
                bb.enabled = clamped > 0
                if (bb.strengthSupported) bb.setStrength(clamped.toShort())
            }
        }
    }

    fun setReverbPreset(preset: Short) {
        _state.update { it.copy(reverbPreset = preset) }
        runCatching {
            presetReverb?.let { rv ->
                rv.enabled = preset != PresetReverb.PRESET_NONE
                if (preset != PresetReverb.PRESET_NONE) rv.preset = preset
            }
        }
    }

    fun updateSpeedAndPitchState(speed: Float, pitch: Float) {
        _state.update { it.copy(playbackSpeed = speed, playbackPitch = pitch) }
    }

    fun updateSleepTimerState(remainingMs: Long, stopAfterSong: Boolean) {
        _state.update {
            it.copy(
                sleepTimerRemainingMs = remainingMs.coerceAtLeast(0L),
                sleepTimerStopAfterSong = stopAfterSong
            )
        }
    }

    fun releaseEffects() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { presetReverb?.release() }
        equalizer = null
        bassBoost = null
        presetReverb = null
        attachedSessionId = 0
    }
}
