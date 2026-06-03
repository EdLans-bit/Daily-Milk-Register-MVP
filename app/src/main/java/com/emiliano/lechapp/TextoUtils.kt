package com.emiliano.lechapp

import kotlin.math.min

object TextoUtils {
    /**
     * Calcula la distancia de Levenshtein entre dos cadenas para medir su similitud.
     * Retorna el número de ediciones (inserciones, borrados, sustituciones) necesarias.
     */
    fun calcularDistanciaLevenshtein(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost)
            }
        }
        return dp[s1.length][s2.length]
    }

    /**
     * Busca el nombre más similar en una lista usando un umbral de tolerancia.
     */
    fun encontrarMasCercano(nombre: String, existentes: List<String>, umbral: Int = 2): String {
        if (existentes.isEmpty()) return nombre
        var mejorMatch = nombre
        var minContexto = Int.MAX_VALUE

        for (ex in existentes) {
            val dist = calcularDistanciaLevenshtein(nombre.lowercase(), ex.lowercase())
            if (dist < minContexto && dist <= umbral) {
                minContexto = dist
                mejorMatch = ex
            }
        }
        return mejorMatch
    }
}
