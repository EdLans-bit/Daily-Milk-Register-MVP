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
        val fechaActual = java.time.LocalDate.now()
        val prompt = """
            Eres un asistente inteligente para ganaderos. Procesa el texto y devuelve un ARRAY de JSON.
            FECHA ACTUAL: $fechaActual.
            
            REGLAS:
            1. MULTIACCIÓN: Si el usuario pide varias cosas (ej. registrar y consultar), devuelve un objeto por cada acción en el array.
            2. TIEMPO RELATIVO: Si dice "ayer", calcula la fecha real basándote en $fechaActual. Devuelve las fechas en formato "YYYY-MM-DD".
            3. NORMALIZACIÓN: Unifica nombres de compradores (ej. "Marco", "Marquitos", "Marcos" -> siempre "Marcos").
            
            FORMATOS DE OBJETO:
            - Registro: {"intencion": "registro", "litros": 10.5, "precio": 2000, "comprador": "Marcos", "fecha": "YYYY-MM-DD"}
            - Consulta: {"intencion": "consulta", "fechaInicio": "YYYY-MM-DD", "fechaFin": "YYYY-MM-DD"}

            Texto: "$textoVoz"
            Responde ÚNICAMENTE con el ARRAY JSON plano: [{}, ...]
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