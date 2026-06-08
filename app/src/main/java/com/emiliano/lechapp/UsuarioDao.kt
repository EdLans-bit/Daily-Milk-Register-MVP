package com.emiliano.lechapp

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarPerfil(perfil: PerfilUsuario)

    @Query("SELECT * FROM perfil_usuario WHERE id = 0")
    suspend fun obtenerPerfil(): PerfilUsuario?

    @Insert
    suspend fun insertarRegistroLeche(registro: RegistroLeche)

    @Query("SELECT * FROM registros_leche ORDER BY fecha DESC")
    fun obtenerTodosLosRegistros(): Flow<List<RegistroLeche>>

    @Transaction
    @Query("SELECT * FROM registros_leche ORDER BY fecha DESC")
    fun obtenerRegistrosConDetalles(): Flow<List<RegistroConDetalles>>

    @Query("SELECT * FROM registros_leche WHERE fecha >= :desde ORDER BY fecha DESC")
    fun obtenerRegistrosFiltrados(desde: Long): Flow<List<RegistroLeche>>

    @Query("SELECT SUM(litros) FROM registros_leche WHERE fecha >= :desde")
    fun obtenerTotalLitrosFiltrado(desde: Long): Flow<Double?>

    @Query("SELECT SUM(litros * precioPorLitro) FROM registros_leche WHERE fecha >= :desde")
    fun obtenerGananciaTotalFiltrada(desde: Long): Flow<Double?>

    @Query("SELECT SUM(litros) FROM registros_leche")
    fun obtenerTotalLitros(): Flow<Double?>

    @Query("SELECT SUM(litros * precioPorLitro) FROM registros_leche")
    fun obtenerGananciaTotal(): Flow<Double?>

    @Query("SELECT SUM(litros * precioPorLitro) FROM registros_leche WHERE fecha BETWEEN :inicio AND :fin")
    suspend fun obtenerGananciaEntreFechas(inicio: Long, fin: Long): Double?

    @Query("SELECT SUM(litros) FROM registros_leche WHERE fecha BETWEEN :inicio AND :fin")
    suspend fun obtenerLitrosEntreFechas(inicio: Long, fin: Long): Double?

    @Delete
    suspend fun borrarRegistro(registro: RegistroLeche)

    @Query("DELETE FROM registros_leche")
    suspend fun borrarTodoElHistorial()
}
