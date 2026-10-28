package com.auramusic.app.playback

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EqBand(
    val index: Short,
    val centerFreqHz: Int,
    val levelMb: Short // milliBels (-1500 to +1500 typically)
)

object AuraAudioEffects {
    private const val TAG = "AuraAudioEffects"
    private const val PREFS_NAME = "aura_audio_fx"

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var prefs: SharedPreferences? = null

    private val _isEnabled = MutableStateFlow(true)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private val _bands = MutableStateFlow<List<EqBand>>(emptyList())
    val bands: StateFlow<List<EqBand>> = _bands.asStateFlow()

    private val _bassBoostStrength = MutableStateFlow(300) // 0 to 1000
    val bassBoostStrength: StateFlow<Int> = _bassBoostStrength.asStateFlow()

    private val _virtualizerStrength = MutableStateFlow(200) // 0 to 1000
    val virtualizerStrength: StateFlow<Int> = _virtualizerStrength.asStateFlow()

    private val _activePreset = MutableStateFlow("Cyberpunk")
    val activePreset: StateFlow<String> = _activePreset.asStateFlow()

    val PRESET_CYBERPUNK = "Cyberpunk"
    val PRESET_SYNTHWAVE = "Synthwave"
    val PRESET_BASS_BOOST = "Bass Boost"
    val PRESET_VOCAL = "Vocal Clarity"
    val PRESET_FLAT = "Flat"
    val PRESET_CUSTOM = "Custom"

    val PRESET_NAMES = listOf(
        PRESET_CYBERPUNK,
        PRESET_SYNTHWAVE,
        PRESET_BASS_BOOST,
        PRESET_VOCAL,
        PRESET_FLAT,
        PRESET_CUSTOM
    )

    fun attach(audioSessionId: Int, context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        try {
            detach()

            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = prefs?.getBoolean("enabled", true) ?: true
            }

            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = equalizer?.enabled == true
                val strength = prefs?.getInt("bass_boost", 300) ?: 300
                setStrength(strength.toShort())
                _bassBoostStrength.value = strength
            }

            virtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = equalizer?.enabled == true
                val strength = prefs?.getInt("virtualizer", 200) ?: 200
                setStrength(strength.toShort())
                _virtualizerStrength.value = strength
            }

            _isEnabled.value = equalizer?.enabled == true

            // Read bands
            equalizer?.let { eq ->
                val numBands = eq.numberOfBands
                val bandList = mutableListOf<EqBand>()
                for (i in 0 until numBands) {
                    val bandIdx = i.toShort()
                    val freq = eq.getCenterFreq(bandIdx) / 1000
                    val savedLevel = prefs?.getInt("band_$i", -9999) ?: -9999
                    val level = if (savedLevel != -9999) {
                        savedLevel.toShort().coerceIn(-1500, 1500)
                    } else {
                        getDefaultCyberpunkGain(i, numBands)
                    }
                    eq.setBandLevel(bandIdx, level)
                    bandList.add(EqBand(bandIdx, freq, level))
                }
                _bands.value = bandList
                _activePreset.value = prefs?.getString("preset", PRESET_CYBERPUNK) ?: PRESET_CYBERPUNK
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach Audio Effects: ${e.message}")
        }
    }

    private fun getDefaultCyberpunkGain(bandIndex: Int, totalBands: Short): Short {
        return when (bandIndex) {
            0 -> 400   // +4dB deep sub bass
            1 -> 250   // +2.5dB mid bass
            2 -> 0     // 0dB mids
            3 -> 300   // +3dB presence
            4 -> 500   // +5dB cyber sparkle
            else -> 0
        }
    }

    fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
        equalizer?.enabled = enabled
        bassBoost?.enabled = enabled
        virtualizer?.enabled = enabled
        prefs?.edit()?.putBoolean("enabled", enabled)?.apply()
    }

    fun setBandLevel(bandIndex: Short, levelMb: Short) {
        val currentList = _bands.value.toMutableList()
        val idx = currentList.indexOfFirst { it.index == bandIndex }
        if (idx != -1) {
            val clamped = levelMb.coerceIn(-1500, 1500)
            currentList[idx] = currentList[idx].copy(levelMb = clamped)
            _bands.value = currentList
            equalizer?.setBandLevel(bandIndex, clamped)
            _activePreset.value = PRESET_CUSTOM
            prefs?.edit()?.apply {
                putInt("band_$bandIndex", clamped.toInt())
                putString("preset", PRESET_CUSTOM)
            }?.apply()
        }
    }

    fun setBassBoost(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _bassBoostStrength.value = clamped
        bassBoost?.setStrength(clamped.toShort())
        prefs?.edit()?.putInt("bass_boost", clamped)?.apply()
    }

    fun setVirtualizer(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _virtualizerStrength.value = clamped
        virtualizer?.setStrength(clamped.toShort())
        prefs?.edit()?.putInt("virtualizer", clamped)?.apply()
    }

    fun applyPreset(presetName: String) {
        _activePreset.value = presetName
        val numBands = _bands.value.size
        val newGains: List<Short> = when (presetName) {
            PRESET_CYBERPUNK -> listOf(400, 250, 0, 300, 500)
            PRESET_SYNTHWAVE -> listOf(300, 400, 100, 50, 250)
            PRESET_BASS_BOOST -> listOf(700, 450, 100, 0, 0)
            PRESET_VOCAL -> listOf(-200, 0, 400, 500, 200)
            PRESET_FLAT -> listOf(0, 0, 0, 0, 0)
            else -> return
        }

        val updatedList = _bands.value.mapIndexed { i, band ->
            val gain = newGains.getOrElse(i) { 0.toShort() }
            equalizer?.setBandLevel(band.index, gain)
            prefs?.edit()?.putInt("band_${band.index}", gain.toInt())?.apply()
            band.copy(levelMb = gain)
        }
        _bands.value = updatedList
        prefs?.edit()?.putString("preset", presetName)?.apply()
    }

    fun detach() {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
        } catch (_: Exception) {}
        equalizer = null
        bassBoost = null
        virtualizer = null
    }
}
