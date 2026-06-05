package com.emiliano.lechapp

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class RankingVaca(
    val idAnimal: Int,
    val identificador: String,
    val totalProduccion: Double,
)
data class BalanceDiario(
    val dia: String,
    val ingresos: Double = 0.0,
    val gastos: Double = 0.0
)

@Dao
interface RegistrosRelacionalesDao {

    // --- Comprador ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComprador(comprador: Comprador): Long

    @Update
    suspend fun updateComprador(comprador: Comprador)

    @Query("SELECT * FROM compradores")
    fun getAllCompradores(): Flow<List<Comprador>>

    @Query("SELECT * FROM compradores WHERE nombre = :nombre LIMIT 1")
    suspend fun findCompradorByName(nombre: String): Comprador?

    @Delete
    suspend fun borrarComprador(comprador: Comprador)

    // --- AnimalLote ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnimalLote(animalLote: AnimalLote): Long

    @Update
    suspend fun updateAnimalLote(animalLote: AnimalLote)

    @Query("SELECT * FROM animales_lotes")
    fun getAllAnimalesLotes(): Flow<List<AnimalLote>>

    @Query("""
        SELECT a.*,
               COALESCE(SUM(r.litros), 0.0) as totalLitros,
               COALESCE(AVG(r.litros), 0.0) as promedioLitros,
               COUNT(r.id) as conteoRegistros,
               COALESCE((SELECT SUM(r2.litros) FROM registros_leche r2 WHERE r2.animalId = a.idAnimal AND date(r2.fecha / 1000, 'unixepoch') = date('now')), 0.0) as litrosHoy
        FROM animales_lotes a 
        LEFT JOIN registros_leche r ON a.idAnimal = r.animalId 
        GROUP BY a.idAnimal
    """)
    fun obtenerAnimalesConProduccionTotal(): Flow<List<AnimalConProduccion>>

    @Query("SELECT * FROM animales_lotes WHERE identificador = :identificador LIMIT 1")
    suspend fun findAnimalLoteByName(identificador: String): AnimalLote?

    @Delete
    suspend fun borrarAnimal(animal: AnimalLote)

    @Query("SELECT * FROM registros_leche WHERE animalId = :animalId AND fecha >= :fechaLimite ORDER BY fecha DESC")
    suspend fun obtenerRegistrosRecientesAnimal(animalId: Int, fechaLimite: Long): List<RegistroLeche>

    // --- Gasto ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGasto(gasto: Gasto): Long

    @Update
    suspend fun updateGasto(gasto: Gasto)

    @Delete
    suspend fun deleteGasto(gasto: Gasto)

    @Query("SELECT * FROM gastos ORDER BY fecha DESC")
    fun getAllGastos(): Flow<List<Gasto>>

    @Query("SELECT COALESCE(SUM(litros * precioPorLitro), 0.0) FROM registros_leche WHERE fecha >= :fechaLimite")
    suspend fun calcularIngresosTotales(fechaLimite: Long): Double

    @Query("SELECT COALESCE(SUM(monto), 0.0) FROM gastos WHERE fecha >= :fechaLimite")
    suspend fun calcularGastosTotales(fechaLimite: Long): Double

    @Query("""
        SELECT 
            COALESCE(strftime('%Y-%m-%d', datetime(fecha / 1000, 'unixepoch', 'localtime')), '1970-01-01') as dia,
            COALESCE(SUM(litros * precioPorLitro), 0.0) as ingresos,
            0.0 as gastos
        FROM registros_leche
        WHERE fecha >= :inicioMs
        GROUP BY dia
        ORDER BY dia ASC
    """)
    fun obtenerIngresosDiarios(inicioMs: Long): Flow<List<BalanceDiario>>

    @Query("""
        SELECT 
            COALESCE(strftime('%Y-%m-%d', datetime(fecha / 1000, 'unixepoch', 'localtime')), '1970-01-01') as dia,
            0.0 as ingresos,
            COALESCE(SUM(monto), 0.0) as gastos
        FROM gastos
        WHERE fecha >= :inicioMs
        GROUP BY dia
        ORDER BY dia ASC
    """)
    fun obtenerGastosDiarios(inicioMs: Long): Flow<List<BalanceDiario>>

    // ====================================================================
    // --- Inteligencia Predictiva y Alertas (Fase 1) ---
    // ====================================================================

    @Query("""
        SELECT COALESCE(SUM(litros), 0.0) as totalLitros 
        FROM registros_leche 
        WHERE fecha BETWEEN :inicioMs AND :finMs 
        AND (:animalIdEspecifico IS NULL OR animalId = :animalIdEspecifico)
        GROUP BY date(fecha / 1000, 'unixepoch', 'localtime')
        ORDER BY fecha ASC
    """)
    suspend fun obtenerHistorialAgrupadoPorDia(
        inicioMs: Long,
        finMs: Long,
        animalIdEspecifico: Int? = null
    ): List<Double>

    @Query("""
        SELECT COUNT(DISTINCT date(fecha / 1000, 'unixepoch')) 
        FROM registros_leche 
        WHERE (:animalIdEspecifico IS NULL OR animalId = :animalIdEspecifico)
    """)
    suspend fun contarDiasConRegistros(animalIdEspecifico: Int? = null): Int

    @Query("""
        SELECT a.idAnimal, a.identificador, COALESCE(SUM(r.litros), 0.0) as totalProduccion
        FROM animales_lotes a
        INNER JOIN registros_leche r ON a.idAnimal = r.animalId
        WHERE r.fecha BETWEEN :inicioMs AND :finMs
        GROUP BY a.idAnimal
        ORDER BY totalProduccion DESC
    """)
    suspend fun obtenerRankingVacas(inicioMs: Long, finMs: Long): List<RankingVaca>
}
