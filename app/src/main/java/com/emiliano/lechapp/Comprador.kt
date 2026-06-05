package com.emiliano.lechapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "compradores")
data class Comprador(
    @PrimaryKey(autoGenerate = true) val idComprador: Int = 0,
    val nombre: String,
    val precioBase: Double,
    val telefono: String? = null,
)
