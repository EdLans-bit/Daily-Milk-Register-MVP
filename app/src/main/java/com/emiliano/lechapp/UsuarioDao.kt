package com.emiliano.lechapp
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UsuarioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarPerfil(perfil: PerfilUsuario)


    @Query("SELECT * FROM perfil_usuario WHERE id = 0")
    suspend fun obtenerPerfil(): PerfilUsuario?
}