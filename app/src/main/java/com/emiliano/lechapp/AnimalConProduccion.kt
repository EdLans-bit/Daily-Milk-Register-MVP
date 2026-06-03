package com.emiliano.lechapp

import androidx.room.Embedded

data class AnimalConProduccion(
    @Embedded val animal: AnimalLote,
    val totalLitros: Double
)
