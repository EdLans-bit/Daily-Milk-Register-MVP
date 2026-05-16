package com.emiliano.lechapp

import java.util.*

/**
 * Extensión para formatear Double a moneda con sufijo 'K' para valores >= 10,000.
 * Ejemplo: 15500.0 -> 15.5K, 5000.0 -> 5,000
 */
fun Double.formatearDinero(): String {
    return if (this >= 10000.0) {
        val miles = this / 1000.0
        if (miles % 1.0 == 0.0) {
            "${miles.toInt()}K"
        } else {
            String.format(Locale.US, "%.1fK", miles)
        }
    } else {
        String.format(Locale.US, "%,.0f", this)
    }
}
