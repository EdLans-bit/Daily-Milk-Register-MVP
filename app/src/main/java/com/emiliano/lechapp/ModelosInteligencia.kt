package com.emiliano.lechapp
enum class NivelAlerta { NORMAL, PRECAUCION, CRITICO }

data class AlertaGenerada(
    val nivel: NivelAlerta,
    val mensaje: String
)

data class ResultadoPredictivo(
    val prediccion: Double,
    val insight: String,
    val alerta: AlertaGenerada? = null,
    val litrosPredichos: Double = 0.0,
    val insightTexto: String = "",
    val porcentajeCaida: Double = 0.0
)