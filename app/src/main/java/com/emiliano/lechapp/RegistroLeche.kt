package com.emiliano.lechapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "registros_leche")
data class RegistroLeche(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val litros: Double,
    val precioPorLitro: Double,
    val fecha: Long = System.currentTimeMillis(), // Guarda el momento exacto
    val notaVoz: String? = null // Aquí guardaremos lo que Gemini procese después
)