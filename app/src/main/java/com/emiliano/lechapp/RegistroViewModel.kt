package com.emiliano.lechapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class RegistroViewModel(private val dao: UsuarioDao) : ViewModel() {

    // Esta función es la que llamaremos cuando el ganadero presione el botón
    fun guardarGanadero(nombre: String, animalesTexto: String) {
        // Convertimos el texto a número (si escribe letras por error, ponemos 0)
        val cantidad = animalesTexto.toIntOrNull() ?: 0

        // Creamos el paquete de datos
        val perfil = PerfilUsuario(
            nombreGanadero = nombre,
            cantidadAnimales = cantidad,
        )

        // Lanzamos el guardado en segundo plano para no congelar la pantalla
        viewModelScope.launch {
            dao.guardarPerfil(perfil)
        }
    }
}