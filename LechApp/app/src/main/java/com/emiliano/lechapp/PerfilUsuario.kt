package com.emiliano.lechapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "perfil_usuario")
data class PerfilUsuario(
    @PrimaryKey val id: Int = 0,
    val nombreGanadero: String,
    val cantidadAnimales: Int
)