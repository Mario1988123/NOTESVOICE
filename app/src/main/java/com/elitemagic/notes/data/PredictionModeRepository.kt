package com.elitemagic.notes.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Repositorio para gestionar la configuración del modo predicción
 * Usado para el truco de magia de "predecir" la fecha/hora de una nota
 */
class PredictionModeRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("prediction_mode", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_PREDICTION_MODE = "is_prediction_mode"
        private const val KEY_CUSTOM_TIMESTAMP = "custom_timestamp"
    }

    /**
     * Activa o desactiva el modo predicción
     */
    fun setPredictionMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_IS_PREDICTION_MODE, enabled).apply()
    }

    /**
     * Verifica si el modo predicción está activado
     */
    fun isPredictionMode(): Boolean {
        return prefs.getBoolean(KEY_IS_PREDICTION_MODE, false)
    }

    /**
     * Guarda la fecha/hora personalizada para el modo predicción
     */
    fun setCustomTimestamp(timestamp: Long) {
        prefs.edit().putLong(KEY_CUSTOM_TIMESTAMP, timestamp).apply()
    }

    /**
     * Obtiene la fecha/hora personalizada
     */
    fun getCustomTimestamp(): Long {
        return prefs.getLong(KEY_CUSTOM_TIMESTAMP, System.currentTimeMillis())
    }

    /**
     * Obtiene el timestamp que se debe usar para una nueva nota
     * Si el modo predicción está activo, devuelve el timestamp personalizado
     * Si no, devuelve el timestamp actual
     */
    fun getTimestampForNewNote(): Long {
        return if (isPredictionMode()) {
            getCustomTimestamp()
        } else {
            System.currentTimeMillis()
        }
    }

    /**
     * Desactiva el modo predicción y limpia la configuración
     */
    fun clearPredictionMode() {
        prefs.edit().clear().apply()
    }
}
