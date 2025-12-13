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
     * Ejemplo: "3 de ♥ Corazones" -> "3_corazones"
     * Ejemplo: "tres de corazones" -> "3_corazones"
     */
    private fun normalizeCardName(cardName: String): String {
        var normalized = cardName.lowercase()
            .replace("á", "a")
            .replace("é", "e")
            .replace("í", "i")
            .replace("ó", "o")
            .replace("ú", "u")

        // Convertir nombres de números a dígitos
        normalized = normalized
            .replace("dos", "2")
            .replace("tres", "3")
            .replace("cuatro", "4")
            .replace("cinco", "5")
            .replace("seis", "6")
            .replace("siete", "7")
            .replace("ocho", "8")
            .replace("nueve", "9")
            .replace("diez", "10")

        // Normalizar nombres de cartas especiales
        normalized = normalized
            .replace("jota", "j")
            .replace("sota", "j")
            .replace("caballo", "q")
            .replace("reina", "q")
            .replace("rey", "k")

        // Normalizar palos
        normalized = normalized
            .replace("♥", "corazones")
            .replace("♦", "diamantes")
            .replace("♣", "treboles")
            .replace("♠", "picas")
            .replace("corazon", "corazones")
            .replace("diamante", "diamantes")
            .replace("trebol", "treboles")
            .replace("pica", "picas")
            .replace("copas", "corazones")
            .replace("copa", "corazones")
            .replace("oros", "diamantes")
            .replace("oro", "diamantes")
            .replace("espadas", "picas")
            .replace("espada", "picas")
            .replace("bastos", "treboles")
            .replace("basto", "treboles")

        // Convertir a formato final: "carta_palo"
        return normalized
            .replace(" de ", "_")
            .replace(" ", "_")
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
