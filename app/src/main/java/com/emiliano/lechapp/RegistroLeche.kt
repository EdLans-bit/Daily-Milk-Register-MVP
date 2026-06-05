package com.emiliano.lechapp

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "registros_leche",
    foreignKeys = [
        ForeignKey(
            entity = Comprador::class,
            parentColumns = ["idComprador"],
            childColumns = ["compradorId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = AnimalLote::class,
            parentColumns = ["idAnimal"],
            childColumns = ["animalId"],
            onDelete = ForeignKey.SET_NULL,
        )
    ],
    indices = [
        Index(value = ["compradorId"]),
        Index(value = ["animalId"])
    ]
)
data class RegistroLeche(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val litros: Double,
    val precioPorLitro: Double,
    val fecha: Long = System.currentTimeMillis(),
    val notaVoz: String? = null,
    val compradorId: Int? = null,
    val animalId: Int? = null,
)
