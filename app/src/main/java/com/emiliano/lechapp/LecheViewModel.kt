package com.emiliano.lechapp

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class LecheViewModel(
    private val dao: UsuarioDao,
    private val geminiService: GeminiService,
) : ViewModel() {

    fun procesarEntradaVoz(context: Context, textoVoz: String) {
        Toast.makeText(context, "Procesando voz...", Toast.LENGTH_SHORT).show()

        viewModelScope.launch {
            if (NetworkUtils.estaOnline(context)) {
                val respuestaIA = geminiService.procesarVozConIA(textoVoz)

                if (respuestaIA != null) {
                    try {
                        val jsonLimpio = respuestaIA.replace("```json", "").replace("```", "").trim()
                        val json = JSONObject(jsonLimpio)
                        val litros = json.getDouble("litros")
                        val precio = json.optDouble("precio", 2000.0)

                        if (litros > 0.0) {
                            val nuevoRegistro = RegistroLeche(litros = litros, precioPorLitro = precio, notaVoz = textoVoz)
                            withContext(Dispatchers.IO) {
                                dao.insertarRegistroLeche(nuevoRegistro)
                            }

                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "¡IA Guardó! $litros L", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Log.w("LechApp_IA", "La IA devolvió 0 litros o un valor inválido. Texto: $textoVoz")
                            procesarOffline(context, textoVoz) // Si entendió 0, pasamos al plan B
                        }
                    } catch (e: Exception) {
                        Log.e("LechApp_IA", "Error procesando JSON de IA: ", e)
                        procesarOffline(context, textoVoz)
                    }
                } else {
                    Log.w("LechApp_IA", "Gemini devolvió NULL (posible fallo de red o API Key)")
                    procesarOffline(context, textoVoz)
                }
            } else {
                Log.i("LechApp_IA", "Sin conexión: usando modo offline directo")
                procesarOffline(context, textoVoz)
            }
        }
    }

    private suspend fun procesarOffline(context: Context, texto: String) {
        val textoLimpio = texto.lowercase()
        Log.d("LechApp_Offline", "Procesando texto: $textoLimpio")
        
        // 1. Regex para detectar números base (ej. "2 litros", "3.5 L")
        // Hemos simplificado la regex para capturar el número antes de "litro/s" o "l"
        val regexLitros = "(\\d+(?:[.,]\\d+)?)\\s*(litros|litro|l\\b)".toRegex()
        val match = regexLitros.find(textoLimpio)
        var litros = match?.groupValues?.get(1)?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
        Log.d("LechApp_Offline", "Litros base detectados: $litros")

        // 2. Lógica aditiva para fracciones (medio, cuarto, medio cuarto)
        var extraFraccion = 0.0
        var textoParaFracciones = textoLimpio

        // Prioridad a "medio cuarto" (0.125)
        if (textoParaFracciones.contains("medio cuarto")) {
            extraFraccion += 0.125
            textoParaFracciones = textoParaFracciones.replace("medio cuarto", "")
            Log.d("LechApp_Offline", "Detectado medio cuarto (+0.125)")
        }

        if (textoParaFracciones.contains("medio")) {
            extraFraccion += 0.5
            Log.d("LechApp_Offline", "Detectado medio (+0.5)")
        }

        if (textoParaFracciones.contains("cuarto")) {
            extraFraccion += 0.25
            Log.d("LechApp_Offline", "Detectado cuarto (+0.25)")
        }

        litros += extraFraccion
        Log.d("LechApp_Offline", "Total litros final: $litros")

        withContext(Dispatchers.Main) {
            if (litros > 0.0) {
                val nuevoRegistro = RegistroLeche(litros = litros, precioPorLitro = 2000.0, notaVoz = texto)
                withContext(Dispatchers.IO) { dao.insertarRegistroLeche(nuevoRegistro) }

                Toast.makeText(context, "Offline Guardó: $litros L", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "No detecté los litros en: '$texto'", Toast.LENGTH_LONG).show()
            }
        }
    }
}