package com.elitemagic.notes.data

import android.content.Context
import android.content.SharedPreferences
import com.elitemagic.notes.model.DrawingPath
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class CardsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("magic_cards", Context.MODE_PRIVATE)
    private val gson = Gson()

    /**
     * Normaliza el nombre de la carta para evitar problemas con mayúsculas/minúsculas
     * Ejemplo: "As de Corazones" -> "as_corazones"
     */
    private fun normalizeCardName(cardName: String): String {
        return cardName.lowercase()
            .replace(" de ", "_")
            .replace(" ", "_")
            .replace("á", "a")
            .replace("é", "e")
            .replace("í", "i")
            .replace("ó", "o")
            .replace("ú", "u")
            .replace("♥", "corazones")
            .replace("♦", "diamantes")
            .replace("♣", "treboles")
            .replace("♠", "picas")
    }

    /**
     * Guarda el dibujo de una carta
     */
    fun saveCardDrawing(cardName: String, paths: List<DrawingPath>) {
        val normalizedName = normalizeCardName(cardName)
        val json = gson.toJson(paths)
        prefs.edit().putString(normalizedName, json).apply()
    }

    /**
     * Obtiene el dibujo guardado de una carta
     */
    fun getCardDrawing(cardName: String): List<DrawingPath>? {
        val normalizedName = normalizeCardName(cardName)
        val json = prefs.getString(normalizedName, null) ?: return null

        return try {
            val type = object : TypeToken<List<DrawingPath>>() {}.type
            gson.fromJson<List<DrawingPath>>(json, type)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Verifica si una carta tiene un dibujo guardado
     */
    fun hasCardDrawing(cardName: String): Boolean {
        val normalizedName = normalizeCardName(cardName)
        return prefs.contains(normalizedName)
    }

    /**
     * Borra el dibujo de una carta
     */
    fun deleteCardDrawing(cardName: String) {
        val normalizedName = normalizeCardName(cardName)
        prefs.edit().remove(normalizedName).apply()
    }

    /**
     * Borra todos los dibujos
     */
    fun clearAllCards() {
        prefs.edit().clear().apply()
    }
}
