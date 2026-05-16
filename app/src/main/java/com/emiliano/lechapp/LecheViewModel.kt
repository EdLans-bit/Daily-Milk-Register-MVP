package com.emiliano.lechapp

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

// 1. Le pasamos el GeminiService al constructor
class LecheViewModel(
    private val dao: UsuarioDao,
    private val geminiService: GeminiService // <-- Nueva pieza del rompecabezas
) : ViewModel() {

    fun procesarEntradaVoz(context: Context, textoVoz: String) {
        viewModelScope.launch {
            if (NetworkUtils.estaOnline(context)) {
                // CAMINO A: Usar la Inteligencia Artificial
                val respuestaIA = geminiService.procesarVozConIA(textoVoz)
                if (respuestaIA != null) {
                    // Aquí luego procesaremos el JSON, por ahora usamos el plan B para probar
                    procesarOffline(textoVoz)
                }
            } else {
                // CAMINO B: Lógica pura sin internet
                procesarOffline(textoVoz)
            }
        }
    }

    private suspend fun procesarOffline(texto: String) {

        val regexLitros = "(\\d+)\\s*(litros|litro|L)".toRegex(RegexOption.IGNORE_CASE)
        val match = regexLitros.find(texto)

        val litros = match?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0

        if (litros > 0.0) {
            val nuevoRegistro = RegistroLeche(
                litros = litros,
                precioPorLitro = 2000.0,
                notaVoz = texto
            )
            dao.insertarRegistroLeche(nuevoRegistro)
        }
    }
}