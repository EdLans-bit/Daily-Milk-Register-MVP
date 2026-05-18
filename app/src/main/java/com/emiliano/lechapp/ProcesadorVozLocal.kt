package com.emiliano.lechapp

import java.util.*

sealed class AccionVoz {
    data class Registro(val registro: RegistroLeche) : AccionVoz()
    data class Consulta(val fechaInicio: Long, val fechaFin: Long) : AccionVoz()
    object NoEntendido : AccionVoz()
}

class ProcesadorVozLocal(private val precioPorDefecto: Double) {

    /**
     * Procesa una frase y determina si es un registro o una consulta estadística.
     */
    fun clasificarIntencion(texto: String): AccionVoz {
        val t = texto.lowercase().trim()

        // 1. Detección de Consulta (Keywords)
        if (t.contains("cuánto") || t.contains("total") || t.contains("plata") || t.contains("gané")) {
            return procesarConsulta(t)
        }

        // 2. Intento de Registro (Plan B)
        val registro = extraerUnicoRegistro(t)
        return if (registro != null) AccionVoz.Registro(registro) else AccionVoz.NoEntendido
    }

    private fun procesarConsulta(texto: String): AccionVoz {
        val cal = Calendar.getInstance()
        
        // Resetear a medianoche para precisión
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        var inicio: Long = cal.timeInMillis
        var fin: Long = System.currentTimeMillis()

        when {
            texto.contains("ayer") -> {
                cal.add(Calendar.DAY_OF_YEAR, -1)
                inicio = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                fin = cal.timeInMillis
            }
            texto.contains("semana") -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                inicio = cal.timeInMillis
            }
            texto.contains("mes") -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                inicio = cal.timeInMillis
            }
            // Por defecto "hoy" si no dice nada específico
        }

        return AccionVoz.Consulta(inicio, fin)
    }

    private fun extraerUnicoRegistro(frase: String): RegistroLeche? {
        val timestamp = extraerFechaRelativa(frase)
        
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

    private fun extraerFechaRelativa(frase: String): Long {
        val cal = Calendar.getInstance()
        when {
            frase.contains("antier") -> cal.add(Calendar.DAY_OF_YEAR, -2)
            frase.contains("ayer") -> cal.add(Calendar.DAY_OF_YEAR, -1)
            // Lógica simplificada para días de la semana
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
        if (diff <= 0) diff += 7
        cal.add(Calendar.DAY_OF_YEAR, -diff)
    }
}
