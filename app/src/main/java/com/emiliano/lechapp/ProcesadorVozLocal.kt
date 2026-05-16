package com.emiliano.lechapp

import java.util.*

class ProcesadorVozLocal(private val precioPorDefecto: Double) {

    fun procesarFrase(texto: String): List<RegistroLeche> {
        val t = texto.lowercase()
        // Dividir por conjunciones para soportar multi-acción
        val subFrases = t.split(" y ", " además ", ", ")
        val registros = mutableListOf<RegistroLeche>()

        for (frase in subFrases) {
            val registro = extraerUnicoRegistro(frase)
            if (registro != null) registros.add(registro)
        }
        return registros
    }

    private fun extraerUnicoRegistro(frase: String): RegistroLeche? {
        val timestamp = extraerFecha(frase)
        
        // Regex: (litros) l/litros [a (comprador)] [a (precio)]
        val regex = "(\\d+(?:[.,]\\d+)?)\\s*(?:litros|litro|l\\b)(?:\\s+a\\s+([\\wáéíóúñ]+))?(?:\\s+a\\s+(\\d+))?".toRegex()
        val match = regex.find(frase) ?: return null

        val litros = match.groupValues[1].replace(",", ".").toDoubleOrNull() ?: 0.0
        val comprador = match.groupValues.getOrNull(2) ?: "General"
        val precio = match.groupValues.getOrNull(3)?.toDoubleOrNull() ?: precioPorDefecto

        if (litros <= 0.0) return null

        return RegistroLeche(
            litros = litros,
            precioPorLitro = precio,
            comprador = comprador.replaceFirstChar { it.uppercase() },
            fecha = timestamp,
            notaVoz = frase.trim()
        )
    }

    private fun extraerFecha(frase: String): Long {
        val cal = Calendar.getInstance()
        when {
            frase.contains("antier") -> cal.add(Calendar.DAY_OF_YEAR, -2)
            frase.contains("ayer") -> cal.add(Calendar.DAY_OF_YEAR, -1)
            frase.contains("lunes") -> ajustarDiaSemana(cal, Calendar.MONDAY)
            frase.contains("martes") -> ajustarDiaSemana(cal, Calendar.TUESDAY)
            frase.contains("miercoles") -> ajustarDiaSemana(cal, Calendar.WEDNESDAY)
            frase.contains("jueves") -> ajustarDiaSemana(cal, Calendar.THURSDAY)
            frase.contains("viernes") -> ajustarDiaSemana(cal, Calendar.FRIDAY)
            frase.contains("sabado") -> ajustarDiaSemana(cal, Calendar.SATURDAY)
            frase.contains("domingo") -> ajustarDiaSemana(cal, Calendar.SUNDAY)
        }
        return cal.timeInMillis
    }

    private fun ajustarDiaSemana(cal: Calendar, diaObjetivo: Int) {
        val hoy = cal.get(Calendar.DAY_OF_WEEK)
        var diff = hoy - diaObjetivo
        if (diff < 0) diff += 7 // Ir a la semana pasada si el día ya pasó
        if (diff == 0) diff = 7 // Si es hoy, asumimos que se refiere al de la semana pasada
        cal.add(Calendar.DAY_OF_YEAR, -diff)
    }
}
