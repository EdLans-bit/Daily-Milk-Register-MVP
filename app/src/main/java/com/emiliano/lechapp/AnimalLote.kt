package com.emiliano.lechapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "animales_lotes")
data class AnimalLote(
    @PrimaryKey(autoGenerate = true) val idAnimal: Int = 0,
    val identificador: String,
    val esLoteGeneral: Boolean = false,
    val raza: String? = null
) {
    companion object {
        val RAZAS_COMUNES = listOf(
            "Holstein",
            "Pardo Suizo",
            "Gyr",
            "Girolando",
            "Jersey",
            "Cruce/Mestizo",
            "Otra"
        )
    }
}
