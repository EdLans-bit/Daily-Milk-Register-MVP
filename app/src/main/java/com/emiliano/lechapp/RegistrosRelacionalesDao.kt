package com.emiliano.lechapp

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RegistrosRelacionalesDao {

    // --- Comprador ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComprador(comprador: Comprador): Long

    @Update
    suspend fun updateComprador(comprador: Comprador)

    @Delete
    suspend fun deleteComprador(comprador: Comprador)

    @Query("SELECT * FROM compradores")
    fun getAllCompradores(): Flow<List<Comprador>>

    @Query("SELECT * FROM compradores WHERE nombre = :nombre LIMIT 1")
    suspend fun findCompradorByName(nombre: String): Comprador?

    // --- AnimalLote ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnimalLote(animalLote: AnimalLote): Long

    @Update
    suspend fun updateAnimalLote(animalLote: AnimalLote)

    @Delete
    suspend fun deleteAnimalLote(animalLote: AnimalLote)

    @Query("SELECT * FROM animales_lotes")
    fun getAllAnimalesLotes(): Flow<List<AnimalLote>>

    @Query("SELECT * FROM animales_lotes WHERE identificador = :identificador LIMIT 1")
    suspend fun findAnimalLoteByName(identificador: String): AnimalLote?

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
}
