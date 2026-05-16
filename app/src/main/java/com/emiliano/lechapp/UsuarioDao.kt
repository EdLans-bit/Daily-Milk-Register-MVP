package com.emiliano.lechapp
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UsuarioDao {
    // --- Lógica de Usuario ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarPerfil(perfil: PerfilUsuario)

    @Query("SELECT * FROM perfil_usuario WHERE id = 0")
    suspend fun obtenerPerfil(): PerfilUsuario?

    // --- Lógica de Leche (NUEVO) ---
    @Insert
    suspend fun insertarRegistroLeche(registro: RegistroLeche)

    @Query("SELECT * FROM registros_leche ORDER BY fecha DESC")
    suspend fun obtenerTodosLosRegistros(): List<RegistroLeche>

    @Query("SELECT SUM(litros) FROM registros_leche")
    suspend fun obtenerTotalLitros(): Double?

    @Query("SELECT SUM(litros * precioPorLitro) FROM registros_leche")
    suspend fun obtenerGananciaTotal(): Double?

    @Query("SELECT * FROM registros_leche ORDER BY fecha DESC LIMIT 10")
    suspend fun obtenerUltimos10Registros(): List<RegistroLeche>
}