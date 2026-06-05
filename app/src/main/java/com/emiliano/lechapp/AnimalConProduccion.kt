package com.emiliano.lechapp

import androidx.room.Embedded

data class AnimalConProduccion(
    @Embedded val animal: AnimalLote,
    val totalLitros: Double,
    val promedioLitros: Double,
    val conteoRegistros: Int,
    val litrosHoy: Double = 0.0
)
