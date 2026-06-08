package com.emiliano.lechapp

import java.util.*

sealed class AccionVoz {
    data class Registro(
        val litros: Double,
        val nombreAnimal: String,
        val esLote: Boolean,
        val nombreComprador: String,
        val precio: Double,
        val fecha: Long,
        val notaVoz: String
    ) : AccionVoz()
    data class Consulta(val fechaInicio: Long, val fechaFin: Long) : AccionVoz()
    object NoEntendido : AccionVoz()
}

class ProcesadorVozLocal(private val precioPorDefecto: Double) {

    /**
     * Procesa una frase y determina si es un registro o una consulta estadística.
     */
    fun clasificarIntencion(texto: String): AccionVoz {
        val t = texto.lowercase().trim()

        // Detección de Consulta (Keywords)
        if (t.contains("cuánto") || t.contains("total") || t.contains("plata") || t.contains("gané")) {
            return procesarConsulta(t)
        }

        // Intento de Registro
        return extraerRegistroMejorado(t) ?: AccionVoz.NoEntendido
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

    private fun extraerRegistroMejorado(frase: String): AccionVoz.Registro? {
        val timestamp = extraerFechaRelativa(frase)
        var textoModificado = frase
        
        // Mapeo extendido de jergas y fracciones de litro
        val reemplazos = mapOf(
            "un litro y medio" to "1.5 litros",
            "litro y medio" to "1.5 litros",
            "un litro y cuarto" to "1.25 litros",
            "litro y cuarto" to "1.25 litros",
            "medio litro" to "0.5 litros",
            "un cuarto de litro" to "0.25 litros",
            "cuarto de litro" to "0.25 litros",
            "tres cuartos de litro" to "0.75 litros",
            "tres cuartos" to "0.75 litros",
            "un litro" to "1 litro",
            "una onza de litro" to "0.029 litros",
            "dos onzas de litro" to "0.059 litros",
            "cuatro onzas de litro" to "0.118 litros",
            "4 onzas de litro" to "0.118 litros"
        )

        for ((key, value) in reemplazos) {
            textoModificado = textoModificado.replace(key, value)
        }

        // Extraer Cantidad de Litros
        val regexLitros = "(\\d+(?:[.,]\\d+)?)\\s*(?:litros|litro|l\\b)".toRegex()
        val matchLitros = regexLitros.find(textoModificado) ?: return null
        val litros = matchLitros.groupValues[1].replace(",", ".").toDoubleOrNull() ?: 0.0
        
        if (litros <= 0.0) return null

        // Extracción de Animal/Lote
        var nombreAnimal = "General"
        var esLote = true

        val regexVaca = "(?:de la vaca|de|vaca)\\s+([\\wáéíóúñ]+)".toRegex()
        val regexLote = "(?:del lote|lote|grupo)\\s+([\\wáéíóúñ]+)".toRegex()

        val matchVaca = regexVaca.find(textoModificado)
        val matchLote = regexLote.find(textoModificado)

        if (matchVaca != null) {
            nombreAnimal = matchVaca.groupValues[1].trim().replaceFirstChar { it.uppercase() }
            esLote = false
        } else if (matchLote != null) {
            nombreAnimal = matchLote.groupValues[1].trim().replaceFirstChar { it.uppercase() }
            esLote = true
        }

        // Extracción de Comprador
        var nombreComprador = "General"
        val regexComprador = "(?:para|al|a)\\s+([\\wáéíóúñ\\s]+?)(?:\\s+a\\s+\\d+|$)".toRegex()
        val matchComprador = regexComprador.find(textoModificado)
        
        if (matchComprador != null) {
            val posibleComprador = matchComprador.groupValues[1].trim()
            if (!posibleComprador.contains("lote") && !posibleComprador.contains("vaca")) {
                nombreComprador = posibleComprador.split(" ").take(2).joinToString(" ")
                    .replaceFirstChar { it.uppercase() }
            }
        }

        // Extracción de Precio
        val regexPrecio = "(?:a|precio)\\s+(\\d+)".toRegex()
        val matchPrecio = regexPrecio.find(textoModificado)
        val precio = matchPrecio?.groupValues?.getOrNull(1)?.toDoubleOrNull() ?: precioPorDefecto

        return AccionVoz.Registro(
            litros = litros,
            nombreAnimal = nombreAnimal,
            esLote = esLote,
            nombreComprador = nombreComprador,
            precio = precio,
            fecha = timestamp,
            notaVoz = frase.trim()
        )
    }

    private fun extraerFechaRelativa(frase: String): Long {
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
        if (diff <= 0) diff += 7
        cal.add(Calendar.DAY_OF_YEAR, -diff)
    }
}
