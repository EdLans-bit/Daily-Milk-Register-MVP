package com.emiliano.lechapp

import android.util.Log // <-- Importante
import com.google.ai.client.generativeai.GenerativeModel

class GeminiService {
    private val apiKey = BuildConfig.GEMINI_API_KEY

    private val model by lazy {
        if (apiKey.isBlank() || apiKey == "null") {
            null
        } else {
            GenerativeModel(
                modelName = "gemini-flash-latest",
                apiKey = apiKey
            )
        }
    }

    suspend fun procesarVozConIA(textoVoz: String): String? {
        val currentModel = model ?: return null
        val fechaActual = java.time.LocalDate.now()
        val prompt = """
            Actúa como un extractor de datos para una aplicación de registro lechero.
            FECHA ACTUAL: $fechaActual.
            
            Analiza el texto y devuelve un ARRAY de JSON.
            
            REGLAS:
            1. MULTIACCIÓN: Si el usuario pide varias cosas (ej. registrar y consultar), devuelve un objeto por cada acción en el array.
            2. TIEMPO RELATIVO: Si dice "ayer", calcula la fecha real basándote en $fechaActual.
            3. NORMALIZACIÓN: Unifica nombres de compradores y animales.
            
            FORMATOS DE OBJETO:
            - Registro Leche:
            {
              "intencion": "registro_leche",
              "litros": Double,
              "identificador_animal": String (Nombre de la vaca o lote. Si no se menciona, devuelve "General"),
              "es_lote": Boolean (true si parece referirse a un grupo o lote, false si es una sola vaca),
              "comprador": String (Nombre del comprador. Si no se menciona, devuelve "General"),
              "precio": Double (Opcional, si se menciona),
              "fecha": "yyyy-MM-dd" (Opcional, si menciona fechas específicas o relativas)
            }
            - Consulta: {"intencion": "consulta", "fechaInicio": "YYYY-MM-DD", "fechaFin": "YYYY-MM-DD"}

            Texto: "$textoVoz"
            Responde ÚNICAMENTE con el ARRAY JSON plano: [{}, ...]
        """.trimIndent()

        return try {
            val response = currentModel.generateContent(prompt)
            // Imprimimos en el Logcat lo que respondió Gemini antes de devolverlo
            Log.d("Lactario_IA", "Respuesta cruda de Gemini: ${response.text}")
            response.text
        } catch (e: Exception) {
            // AQUÍ DESTAPAMOS EL ERROR:
            Log.e("Lactario_IA", "¡Falló Gemini! Motivo: ${e.message}", e)
            null
        }
    }
}