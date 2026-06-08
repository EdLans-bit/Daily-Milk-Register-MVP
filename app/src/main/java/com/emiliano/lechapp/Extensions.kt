package com.emiliano.lechapp

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

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
