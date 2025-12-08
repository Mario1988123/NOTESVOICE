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

    private var speechRecognizer: SpeechRecognizer? = null
    private var isWaitingForMagicCommand = false
    private var fullTranscript = StringBuilder()

    // Magic commands that trigger the special recognition
    private val magicCommands = listOf(
        "tu carta pensada es",
        "tu palabra pensada es",
        "la carta pensada es",
        "la palabra pensada es"
    )

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
            if (isWaitingForMagicCommand && error != SpeechRecognizer.ERROR_CLIENT) {
                // Restart after a brief delay
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    if (isWaitingForMagicCommand) {
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
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            matches?.let {
                Log.d(TAG, "Partial results: $it")
                // For continuous listening, we can check partial results
                if (isWaitingForMagicCommand) {
                    processPartialResults(it)
                }
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
            // Reserved for future use
        }
    }

    private fun processPartialResults(matches: List<String>) {
        for (match in matches) {
            val lowerMatch = match.lowercase(Locale.getDefault())
            fullTranscript.append(" ").append(lowerMatch)

            // Check if any magic command is present
            for (command in magicCommands) {
                if (lowerMatch.contains(command)) {
                    Log.d(TAG, "Magic command detected in partial: $command")
                    // We'll wait for final results to extract the word
                }
            }
        }
    }

    private fun processSpeechResults(matches: List<String>) {
        if (matches.isEmpty()) {
            restartListeningIfNeeded()
            return
        }

        val bestMatch = matches[0]
        val lowerMatch = bestMatch.lowercase(Locale.getDefault())

        Log.d(TAG, "Speech results: $bestMatch")
        fullTranscript.append(" ").append(lowerMatch)

        if (isWaitingForMagicCommand) {
            // Check if the magic command is present
            var foundCommand = false
            for (command in magicCommands) {
                if (lowerMatch.contains(command)) {
                    foundCommand = true
                    // Extract the word/card after the magic command
                    val parts = lowerMatch.split(command)
                    if (parts.size > 1) {
                        val wordAfterCommand = parts[1].trim().split(" ").firstOrNull()
                        if (!wordAfterCommand.isNullOrEmpty()) {
                            Log.d(TAG, "Extracted word: $wordAfterCommand")
                            _recognizedText.value = wordAfterCommand
                            _isListening.value = false
                            isWaitingForMagicCommand = false
                            stopListening()
                            return
                        }
                    }
                    break
                }
            }

            // If no command found, keep listening
            if (!foundCommand) {
                restartListeningIfNeeded()
            }
        } else {
            // Normal mode - just return the recognized text
            _recognizedText.value = bestMatch
            _isListening.value = false
        }
    }

    private fun restartListeningIfNeeded() {
        if (isWaitingForMagicCommand) {
            // Restart listening after a short delay
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (isWaitingForMagicCommand) {
                    startListening()
                }
            }, 100)
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
        isWaitingForMagicCommand = true
        fullTranscript.clear()
        _recognizedText.value = null
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
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000)
        }

        _error.value = null
        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        isWaitingForMagicCommand = false
        _isListening.value = false
        speechRecognizer?.stopListening()
    }

    fun destroy() {
        isWaitingForMagicCommand = false
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
