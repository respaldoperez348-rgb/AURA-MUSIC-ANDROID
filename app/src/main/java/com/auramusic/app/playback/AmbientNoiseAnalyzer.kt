package com.auramusic.app.playback

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

enum class NoiseType {
    QUIET,
    TRAFFIC,
    CROWD,
    HIGH_NOISE
}

data class StreetDspStatus(
    val isEnabled: Boolean = false,
    val isHeadphonesConnected: Boolean = false,
    val isSampling: Boolean = false,
    val noiseType: NoiseType = NoiseType.QUIET,
    val statusLabel: String = "Silencio / Entorno Óptimo",
    val detailLabel: String = "Respuesta plana / Sin compensación",
    val detectedDb: Float = 0f,
    val compensationsMb: List<Float> = listOf(0f, 0f, 0f, 0f, 0f) // millibels compensation for the 5 bands
)

object AmbientNoiseAnalyzer {
    private const val TAG = "AmbientNoiseAnalyzer"
    private const val PREFS_NAME = "aura_street_dsp_prefs"
    private const val KEY_ENABLED = "street_dsp_enabled"

    private const val SAMPLE_RATE = 44100
    private const val FFT_SIZE = 4096 // 4096 samples @ 44.1kHz = ~93ms window, 10.7Hz resolution
    private const val DUTY_CYCLE_RECORD_MS = 400L
    private const val DUTY_CYCLE_SLEEP_MS = 6000L

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var analysisJob: Job? = null
    private var appContext: Context? = null
    private var prefs: SharedPreferences? = null

    private val _status = MutableStateFlow(StreetDspStatus())
    val status: StateFlow<StreetDspStatus> = _status.asStateFlow()

    private val currentOffsets = FloatArray(5) { 0f }
    private var isPlayingState: Boolean = false

    fun init(context: Context) {
        appContext = context.applicationContext
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedEnabled = prefs?.getBoolean(KEY_ENABLED, false) ?: false
        val hasHeadphones = checkHeadphonesConnected(context)
        _status.update {
            it.copy(
                isEnabled = savedEnabled,
                isHeadphonesConnected = hasHeadphones
            )
        }
        if (savedEnabled) {
            startAnalysis()
        }
    }

    fun setPlaybackState(isPlaying: Boolean) {
        isPlayingState = isPlaying
    }

    fun setEnabled(enabled: Boolean, context: Context): Boolean {
        appContext = context.applicationContext
        prefs?.edit()?.putBoolean(KEY_ENABLED, enabled)?.apply()

        if (enabled) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                return false
            }

            val hasHeadphones = checkHeadphonesConnected(context)
            _status.update {
                it.copy(
                    isEnabled = true,
                    isHeadphonesConnected = hasHeadphones
                )
            }
            startAnalysis()
            return true
        } else {
            _status.update { it.copy(isEnabled = false, isSampling = false) }
            stopAnalysis()
            return true
        }
    }

    fun checkHeadphonesConnected(context: Context): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        val connected = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            devices.any { device ->
                when (device.type) {
                    AudioDeviceInfo.TYPE_WIRED_HEADSET,
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                    AudioDeviceInfo.TYPE_USB_HEADSET,
                    AudioDeviceInfo.TYPE_USB_DEVICE -> true
                    else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        device.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                        device.type == AudioDeviceInfo.TYPE_BLE_SPEAKER
                    } else false
                }
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager.isWiredHeadsetOn || audioManager.isBluetoothA2dpOn
        }
        _status.update { it.copy(isHeadphonesConnected = connected) }
        return connected
    }

    private fun startAnalysis() {
        analysisJob?.cancel()
        analysisJob = scope.launch {
            while (isActive && _status.value.isEnabled) {
                val context = appContext
                if (context == null) {
                    delay(1000)
                    continue
                }

                val hasHeadphones = checkHeadphonesConnected(context)
                if (!hasHeadphones) {
                    _status.update {
                        it.copy(
                            isHeadphonesConnected = false,
                            statusLabel = "Auriculares requeridos",
                            detailLabel = "Pausado para evitar acople acústico",
                            isSampling = false
                        )
                    }
                    smoothDecayToZero()
                    delay(2000)
                    continue
                }

                // Duty-cycling: Record 400ms, then sleep 6000ms
                val sampleResult = sampleAmbientNoise(context)
                if (sampleResult != null) {
                    val (bandEnergies, overallDb) = sampleResult
                    val (noiseType, statusTitle, statusDesc, targetOffsets) = evaluateUnmaskingCurve(bandEnergies, overallDb)

                    _status.update {
                        it.copy(
                            noiseType = noiseType,
                            statusLabel = statusTitle,
                            detailLabel = statusDesc,
                            detectedDb = overallDb,
                            isSampling = false
                        )
                    }

                    // Smooth Exponential Moving Average transition to avoid sudden acoustic jumps
                    smoothTransitionToTarget(targetOffsets)
                } else {
                    _status.update { it.copy(isSampling = false) }
                }

                // Sleep for duty cycle pause
                delay(DUTY_CYCLE_SLEEP_MS)
            }
        }
    }

    private fun stopAnalysis() {
        analysisJob?.cancel()
        analysisJob = scope.launch {
            smoothDecayToZero()
            _status.update {
                it.copy(
                    isSampling = false,
                    noiseType = NoiseType.QUIET,
                    statusLabel = "DSP Desactivado",
                    detailLabel = "Respuesta de audio estándar",
                    compensationsMb = listOf(0f, 0f, 0f, 0f, 0f)
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun sampleAmbientNoise(context: Context): Pair<FloatArray, Float>? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return null
        }

        val minBufSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBufSize <= 0) return null

        val bufferSize = max(minBufSize, FFT_SIZE * 2)
        var recorder: AudioRecord? = null

        return try {
            _status.update { it.copy(isSampling = true) }
            recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                recorder.release()
                return null
            }

            recorder.startRecording()
            val audioBuffer = ShortArray(FFT_SIZE)
            var readCount = 0

            // Read until FFT_SIZE is filled or 400ms passes
            val startTime = System.currentTimeMillis()
            while (readCount < FFT_SIZE && (System.currentTimeMillis() - startTime) < DUTY_CYCLE_RECORD_MS) {
                val read = recorder.read(audioBuffer, readCount, FFT_SIZE - readCount)
                if (read > 0) readCount += read
                else delay(10)
            }

            recorder.stop()
            recorder.release()
            recorder = null

            if (readCount < FFT_SIZE / 2) return null

            // Windowing and FFT
            val real = FloatArray(FFT_SIZE)
            val imag = FloatArray(FFT_SIZE)
            for (i in 0 until FFT_SIZE) {
                val rawSample = if (i < readCount) audioBuffer[i].toFloat() / 32768.0f else 0.0f
                // Hann Window to reduce spectral leakage
                val window = 0.5f * (1.0f - cos(2.0 * Math.PI * i / (FFT_SIZE - 1))).toFloat()
                real[i] = rawSample * window
                imag[i] = 0f
            }

            FastFourierTransform.compute(real, imag)

            // Calculate band energies
            // Frequency resolution = 44100 / 4096 = 10.766 Hz per bin
            val binResolution = SAMPLE_RATE.toFloat() / FFT_SIZE
            val halfSize = FFT_SIZE / 2

            // 5 EQ Bands centered around: 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz
            // Band 0: 20Hz - 140Hz
            // Band 1: 140Hz - 500Hz
            // Band 2: 500Hz - 1800Hz
            // Band 3: 1800Hz - 7000Hz
            // Band 4: 7000Hz - 20000Hz
            val bandSum = FloatArray(5)
            val bandCount = IntArray(5)
            var totalPower = 0.0f

            for (k in 1 until halfSize) {
                val freq = k * binResolution
                val magnitude = sqrt(real[k] * real[k] + imag[k] * imag[k])
                val power = magnitude * magnitude
                totalPower += power

                when {
                    freq in 20.0f..140.0f -> { bandSum[0] += power; bandCount[0]++ }
                    freq in 140.0f..500.0f -> { bandSum[1] += power; bandCount[1]++ }
                    freq in 500.0f..1800.0f -> { bandSum[2] += power; bandCount[2]++ }
                    freq in 1800.0f..7000.0f -> { bandSum[3] += power; bandCount[3]++ }
                    freq in 7000.0f..20000.0f -> { bandSum[4] += power; bandCount[4]++ }
                }
            }

            val bandDb = FloatArray(5)
            for (b in 0 until 5) {
                val avgPower = if (bandCount[b] > 0) bandSum[b] / bandCount[b] else 1e-12f
                // Relative calibrated dB SPL approximation
                bandDb[b] = (10.0f * log10(max(avgPower, 1e-12f)) + 90.0f).coerceIn(10.0f, 100.0f)
            }

            val overallDb = (10.0f * log10(max(totalPower / halfSize, 1e-12f)) + 90.0f).coerceIn(10.0f, 100.0f)

            Pair(bandDb, overallDb)
        } catch (e: Exception) {
            Log.e(TAG, "AudioRecord sampling error: ${e.message}")
            recorder?.runCatching {
                stop()
                release()
            }
            null
        }
    }

    private fun evaluateUnmaskingCurve(
        bandDb: FloatArray,
        overallDb: Float
    ): Quadruple<NoiseType, String, String, FloatArray> {
        val lowNoise = (bandDb[0] + bandDb[1]) / 2f
        val midNoise = (bandDb[2] + bandDb[3]) / 2f

        return when {
            // Severe wideband urban noise (subway, sirens, dense city construction)
            overallDb >= 66f -> {
                Quadruple(
                    NoiseType.HIGH_NOISE,
                    "Ruido Callejero Intenso",
                    "Compensación total contra rugido exterior (+Bass/Presence)",
                    floatArrayOf(450f, 350f, 200f, 400f, 250f)
                )
            }
            // Low-frequency dominant noise (bus, train, car engines, airplane cabin)
            lowNoise >= 53f && lowNoise > midNoise + 3f -> {
                val boostB0 = (200f + (lowNoise - 50f) * 15f).coerceIn(250f, 450f)
                val boostB1 = (150f + (lowNoise - 50f) * 10f).coerceIn(200f, 350f)
                Quadruple(
                    NoiseType.TRAFFIC,
                    "Motor / Tráfico Urbano",
                    "Desmascarando frecuencias graves y presencia vocal",
                    floatArrayOf(boostB0, boostB1, 80f, 220f, 50f)
                )
            }
            // Mid-frequency dominant noise (crowd chatter, cafeteria, restaurant, office)
            midNoise >= 50f -> {
                val boostVocal = (200f + (midNoise - 48f) * 15f).coerceIn(250f, 400f)
                Quadruple(
                    NoiseType.CROWD,
                    "Multitud / Voces Detectadas",
                    "Realce de inteligibilidad vocal y brillo acústico",
                    floatArrayOf(100f, 50f, 150f, boostVocal, 250f)
                )
            }
            // Quiet environment (room, bedroom, library)
            else -> {
                Quadruple(
                    NoiseType.QUIET,
                    "Silencio / Entorno Óptimo",
                    "Frecuencias balanceadas sin compensación artificial",
                    floatArrayOf(0f, 0f, 0f, 0f, 0f)
                )
            }
        }
    }

    private suspend fun smoothTransitionToTarget(target: FloatArray) {
        val steps = 15
        val stepDelayMs = 50L
        for (step in 1..steps) {
            val alpha = 0.20f
            for (i in 0 until 5) {
                currentOffsets[i] = currentOffsets[i] * (1f - alpha) + target[i] * alpha
            }
            AuraAudioEffects.updateAdaptiveOffsets(currentOffsets)
            _status.update {
                it.copy(compensationsMb = currentOffsets.toList())
            }
            delay(stepDelayMs)
        }
    }

    private suspend fun smoothDecayToZero() {
        val steps = 10
        val stepDelayMs = 40L
        for (step in 1..steps) {
            for (i in 0 until 5) {
                currentOffsets[i] *= 0.65f
                if (currentOffsets[i] < 5f) currentOffsets[i] = 0f
            }
            AuraAudioEffects.updateAdaptiveOffsets(currentOffsets)
            _status.update {
                it.copy(compensationsMb = currentOffsets.toList())
            }
            delay(stepDelayMs)
        }
    }
}

/**
 * Pure Kotlin in-place Radix-2 Cooley-Tukey Fast Fourier Transform (DIT)
 * Extremely fast, zero JNI overhead, runs in < 1ms on Android ARM.
 */
object FastFourierTransform {
    fun compute(real: FloatArray, imag: FloatArray) {
        val n = real.size
        require(n and (n - 1) == 0) { "FFT size must be a power of 2" }

        // Bit-reversal permutation
        var j = 0
        for (i in 0 until n - 1) {
            if (i < j) {
                val tempR = real[i]; real[i] = real[j]; real[j] = tempR
                val tempI = imag[i]; imag[i] = imag[j]; imag[j] = tempI
            }
            var k = n shr 1
            while (k <= j) {
                j -= k
                k = k shr 1
            }
            j += k
        }

        // Cooley-Tukey butterfly computations
        var len = 2
        while (len <= n) {
            val halfLen = len shr 1
            val angle = -2.0 * Math.PI / len
            val wStepR = cos(angle).toFloat()
            val wStepI = sin(angle).toFloat()

            var i = 0
            while (i < n) {
                var wR = 1.0f
                var wI = 0.0f
                for (m in 0 until halfLen) {
                    val idxA = i + m
                    val idxB = idxA + halfLen
                    val uR = real[idxA]
                    val uI = imag[idxA]
                    val vR = real[idxB] * wR - imag[idxB] * wI
                    val vI = real[idxB] * wI + imag[idxB] * wR

                    real[idxA] = uR + vR
                    imag[idxA] = uI + vI
                    real[idxB] = uR - vR
                    imag[idxB] = uI - vI

                    val nextWR = wR * wStepR - wI * wStepI
                    val nextWI = wR * wStepI + wI * wStepR
                    wR = nextWR
                    wI = nextWI
                }
                i += len
            }
            len = len shl 1
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

