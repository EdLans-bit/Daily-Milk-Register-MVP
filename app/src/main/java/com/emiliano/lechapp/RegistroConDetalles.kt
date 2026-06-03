package com.emiliano.lechapp

import androidx.room.Embedded
import androidx.room.Relation

data class RegistroConDetalles(
    @Embedded val registro: RegistroLeche,
    @Relation(
        parentColumn = "compradorId",
        entityColumn = "idComprador"
    )
    val comprador: Comprador?,
    @Relation(
        parentColumn = "animalId",
        entityColumn = "idAnimal"
    )
    val animalLote: AnimalLote?
)
