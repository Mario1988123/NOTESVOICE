package com.elitemagic.notes.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class VoiceRecognitionManager(private val context: Context) {

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _recognizedText = MutableStateFlow<String?>(null)
    val recognizedText: StateFlow<String?> = _recognizedText.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _microphoneStartTime = MutableStateFlow<Long?>(null)
    val microphoneStartTime: StateFlow<Long?> = _microphoneStartTime.asStateFlow()

    private val _partialText = MutableStateFlow<String?>(null)
    val partialText: StateFlow<String?> = _partialText.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var isWaitingForContinuousListening = false

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "Ready for speech")
            _isListening.value = true
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "Beginning of speech")
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Voice volume changed
        }

        override fun onBufferReceived(buffer: ByteArray?) {
            // Audio buffer received
        }

        override fun onEndOfSpeech() {
            Log.d(TAG, "End of speech")
        }

        override fun onError(error: Int) {
            val errorMessage = getErrorText(error)
            Log.e(TAG, "Error: $errorMessage")
            _error.value = errorMessage
            _isListening.value = false

            // Restart listening if we're in continuous mode and it wasn't a manual stop
            if (isWaitingForContinuousListening && error != SpeechRecognizer.ERROR_CLIENT) {
                // Restart after a brief delay
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    if (isWaitingForContinuousListening) {
                        startListening()
                    }
                }, 500)
            }
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            matches?.let { processSpeechResults(it) }
        }

        override fun onPartialResults(results: Bundle?) {
            // Partial results can be used for real-time display if needed
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            matches?.let {
                Log.d(TAG, "Partial results: $it")
                if (it.isNotEmpty()) {
                    _partialText.value = it[0]
                }
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
            // Reserved for future use
        }
    }

    private fun processSpeechResults(matches: List<String>) {
        if (matches.isEmpty()) {
            restartListeningIfNeeded()
            return
        }

        val bestMatch = matches[0]
        Log.d(TAG, "Speech results: $bestMatch")

        // Emitir todo el texto reconocido
        _recognizedText.value = bestMatch

        // Restart listening for continuous mode
        restartListeningIfNeeded()
    }

    private fun restartListeningIfNeeded() {
        if (isWaitingForContinuousListening) {
            // Restart listening after a delay
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (isWaitingForContinuousListening) {
                    startListening()
                }
            }, 1000)
        }
    }

    private fun getErrorText(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Error de audio"
            SpeechRecognizer.ERROR_CLIENT -> "Error del cliente"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permisos insuficientes"
            SpeechRecognizer.ERROR_NETWORK -> "Error de red"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Tiempo de espera agotado"
            SpeechRecognizer.ERROR_NO_MATCH -> "No se encontró coincidencia"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Reconocedor ocupado"
            SpeechRecognizer.ERROR_SERVER -> "Error del servidor"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No se detectó voz"
            else -> "Error desconocido"
        }
    }

    fun startContinuousListening() {
        isWaitingForContinuousListening = true
        _recognizedText.value = null
        _microphoneStartTime.value = System.currentTimeMillis()
        startListening()
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _error.value = "El reconocimiento de voz no está disponible"
            return
        }

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(recognitionListener)
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            // Tiempo de silencio moderado
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000)
            // Intentar evitar sonidos del sistema (no siempre funciona)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }

        _error.value = null
        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        isWaitingForContinuousListening = false
        _isListening.value = false
        _microphoneStartTime.value = null
        speechRecognizer?.stopListening()
    }

    fun destroy() {
        isWaitingForContinuousListening = false
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    fun clearRecognizedText() {
        _recognizedText.value = null
    }

    companion object {
        private const val TAG = "VoiceRecognitionManager"
    }
}
