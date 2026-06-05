package com.emiliano.lechapp

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

/**
 * Extensión para formatear Double a moneda con punto como separador de miles.
 * Ejemplo: 15500.0 -> 15.500
 */
fun Double.formatearMiles(): String {
    val symbols = DecimalFormatSymbols(Locale.getDefault())
    symbols.groupingSeparator = '.'
    symbols.decimalSeparator = ','
    val df = DecimalFormat("#,###", symbols)
    return df.format(this)
}

fun Double.formatearDinero(): String {
    return "$ ${this.formatearMiles()}"
}
