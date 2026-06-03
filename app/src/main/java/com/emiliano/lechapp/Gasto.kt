package com.emiliano.lechapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gastos")
data class Gasto(
    @PrimaryKey(autoGenerate = true) val idGasto: Int = 0,
    val concepto: String,
    val monto: Double,
    val fecha: Long = System.currentTimeMillis()
)
