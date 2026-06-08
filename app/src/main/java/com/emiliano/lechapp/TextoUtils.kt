package com.emiliano.lechapp

import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.graphics.Typeface
import kotlin.math.min

object TextoUtils {
    
    fun formatearMarkdown(texto: String): SpannableStringBuilder {
        val ssb = SpannableStringBuilder()
        var i = 0
        while (i < texto.length) {
            if (texto.startsWith("**", i)) {
                val fin = texto.indexOf("**", i + 2)
                if (fin != -1) {
                    val start = ssb.length
                    ssb.append(texto.substring(i + 2, fin))
                    ssb.setSpan(StyleSpan(Typeface.BOLD), start, ssb.length, 0)
                    i = fin + 2
                    continue
                }
            }
            ssb.append(texto[i])
            i++
        }
        return ssb
    }

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

    fun encontrarMasCercano(nombre: String, existentes: List<String>, umbral: Int = 2): String {
        if (existentes.isEmpty()) return nombre
        var mejorMatch = nombre
        var minContexto = Int.MAX_VALUE

        for (ex in existentes) {
            val dist = calcularDistanciaLevenshtein(nombre.lowercase(), ex.lowercase())
            if ((dist < minContexto) && (dist <= umbral)) {
                minContexto = dist
                mejorMatch = ex
            }
        }
        return mejorMatch
    }
}
