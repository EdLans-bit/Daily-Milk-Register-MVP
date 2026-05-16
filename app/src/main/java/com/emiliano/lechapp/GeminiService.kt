package com.emiliano.lechapp

import android.util.Log // <-- Importante
import com.google.ai.client.generativeai.GenerativeModel

class GeminiService {
    private val apiKey = BuildConfig.GEMINI_API_KEY

    private val model = GenerativeModel(
        modelName = "gemini-flash-latest",
        apiKey = apiKey
    )

    suspend fun procesarVozConIA(textoVoz: String): String? {
        val prompt = """
            Eres un asistente contable para ganaderos. Tu tarea es extraer datos de producción.
            Del siguiente texto, extrae la cantidad de LITROS de leche y el PRECIO por litro.
            Responde ÚNICAMENTE en formato JSON plano, sin Markdown, así:
            {"litros": 0.0, "precio": 0.0}
            Si no hay precio, usa 2000.0 como valor predeterminado.
            Texto: "$textoVoz"
        """.trimIndent()

        return try {
            val response = model.generateContent(prompt)
            // Imprimimos en el Logcat lo que respondió Gemini antes de devolverlo
            Log.d("LechApp_IA", "Respuesta cruda de Gemini: ${response.text}")
            response.text
        } catch (e: Exception) {
            // AQUÍ DESTAPAMOS EL ERROR:
            Log.e("LechApp_IA", "¡Falló Gemini! Motivo: ${e.message}", e)
            null
        }
    }
}