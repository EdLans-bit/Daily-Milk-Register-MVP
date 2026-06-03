package com.emiliano.lechapp

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Double
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class UsuarioDao_Impl(
  __db: RoomDatabase,
) : UsuarioDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfPerfilUsuario: EntityInsertAdapter<PerfilUsuario>

  private val __insertAdapterOfRegistroLeche: EntityInsertAdapter<RegistroLeche>

  private val __deleteAdapterOfRegistroLeche: EntityDeleteOrUpdateAdapter<RegistroLeche>
  init {
    this.__db = __db
    this.__insertAdapterOfPerfilUsuario = object : EntityInsertAdapter<PerfilUsuario>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `perfil_usuario` (`id`,`nombreGanadero`,`cantidadAnimales`) VALUES (?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: PerfilUsuario) {
        statement.bindLong(1, entity.id.toLong())
        statement.bindText(2, entity.nombreGanadero)
        statement.bindLong(3, entity.cantidadAnimales.toLong())
      }
    }
    this.__insertAdapterOfRegistroLeche = object : EntityInsertAdapter<RegistroLeche>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `registros_leche` (`id`,`litros`,`precioPorLitro`,`fecha`,`notaVoz`,`comprador`) VALUES (nullif(?, 0),?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: RegistroLeche) {
        statement.bindLong(1, entity.id.toLong())
        statement.bindDouble(2, entity.litros)
        statement.bindDouble(3, entity.precioPorLitro)
        statement.bindLong(4, entity.fecha)
        val _tmpNotaVoz: String? = entity.notaVoz
        if (_tmpNotaVoz == null) {
          statement.bindNull(5)
        } else {
          statement.bindText(5, _tmpNotaVoz)
        }
        statement.bindText(6, entity.comprador)
      }
    }
    this.__deleteAdapterOfRegistroLeche = object : EntityDeleteOrUpdateAdapter<RegistroLeche>() {
      protected override fun createQuery(): String = "DELETE FROM `registros_leche` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: RegistroLeche) {
        statement.bindLong(1, entity.id.toLong())
      }
    }
  }

  public override suspend fun guardarPerfil(perfil: PerfilUsuario): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfPerfilUsuario.insert(_connection, perfil)
  }

  public override suspend fun insertarRegistroLeche(registro: RegistroLeche): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfRegistroLeche.insert(_connection, registro)
  }

  public override suspend fun borrarRegistro(registro: RegistroLeche): Unit = performSuspending(__db, false, true) { _connection ->
    __deleteAdapterOfRegistroLeche.handle(_connection, registro)
  }

  public override suspend fun obtenerPerfil(): PerfilUsuario? {
    val _sql: String = "SELECT * FROM perfil_usuario WHERE id = 0"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfNombreGanadero: Int = getColumnIndexOrThrow(_stmt, "nombreGanadero")
        val _columnIndexOfCantidadAnimales: Int = getColumnIndexOrThrow(_stmt, "cantidadAnimales")
        val _result: PerfilUsuario?
        if (_stmt.step()) {
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpNombreGanadero: String
          _tmpNombreGanadero = _stmt.getText(_columnIndexOfNombreGanadero)
          val _tmpCantidadAnimales: Int
          _tmpCantidadAnimales = _stmt.getLong(_columnIndexOfCantidadAnimales).toInt()
          _result = PerfilUsuario(_tmpId,_tmpNombreGanadero,_tmpCantidadAnimales)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun obtenerTodosLosRegistros(): Flow<List<RegistroLeche>> {
    val _sql: String = "SELECT * FROM registros_leche ORDER BY fecha DESC"
    return createFlow(__db, false, arrayOf("registros_leche")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfLitros: Int = getColumnIndexOrThrow(_stmt, "litros")
        val _columnIndexOfPrecioPorLitro: Int = getColumnIndexOrThrow(_stmt, "precioPorLitro")
        val _columnIndexOfFecha: Int = getColumnIndexOrThrow(_stmt, "fecha")
        val _columnIndexOfNotaVoz: Int = getColumnIndexOrThrow(_stmt, "notaVoz")
        val _columnIndexOfComprador: Int = getColumnIndexOrThrow(_stmt, "comprador")
        val _result: MutableList<RegistroLeche> = mutableListOf()
        while (_stmt.step()) {
          val _item: RegistroLeche
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpLitros: Double
          _tmpLitros = _stmt.getDouble(_columnIndexOfLitros)
          val _tmpPrecioPorLitro: Double
          _tmpPrecioPorLitro = _stmt.getDouble(_columnIndexOfPrecioPorLitro)
          val _tmpFecha: Long
          _tmpFecha = _stmt.getLong(_columnIndexOfFecha)
          val _tmpNotaVoz: String?
          if (_stmt.isNull(_columnIndexOfNotaVoz)) {
            _tmpNotaVoz = null
          } else {
            _tmpNotaVoz = _stmt.getText(_columnIndexOfNotaVoz)
          }
          val _tmpComprador: String
          _tmpComprador = _stmt.getText(_columnIndexOfComprador)
          _item = RegistroLeche(_tmpId,_tmpLitros,_tmpPrecioPorLitro,_tmpFecha,_tmpNotaVoz,_tmpComprador)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun obtenerRegistrosFiltrados(desde: Long): Flow<List<RegistroLeche>> {
    val _sql: String = "SELECT * FROM registros_leche WHERE fecha >= ? ORDER BY fecha DESC"
    return createFlow(__db, false, arrayOf("registros_leche")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, desde)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfLitros: Int = getColumnIndexOrThrow(_stmt, "litros")
        val _columnIndexOfPrecioPorLitro: Int = getColumnIndexOrThrow(_stmt, "precioPorLitro")
        val _columnIndexOfFecha: Int = getColumnIndexOrThrow(_stmt, "fecha")
        val _columnIndexOfNotaVoz: Int = getColumnIndexOrThrow(_stmt, "notaVoz")
        val _columnIndexOfComprador: Int = getColumnIndexOrThrow(_stmt, "comprador")
        val _result: MutableList<RegistroLeche> = mutableListOf()
        while (_stmt.step()) {
          val _item: RegistroLeche
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpLitros: Double
          _tmpLitros = _stmt.getDouble(_columnIndexOfLitros)
          val _tmpPrecioPorLitro: Double
          _tmpPrecioPorLitro = _stmt.getDouble(_columnIndexOfPrecioPorLitro)
          val _tmpFecha: Long
          _tmpFecha = _stmt.getLong(_columnIndexOfFecha)
          val _tmpNotaVoz: String?
          if (_stmt.isNull(_columnIndexOfNotaVoz)) {
            _tmpNotaVoz = null
          } else {
            _tmpNotaVoz = _stmt.getText(_columnIndexOfNotaVoz)
          }
          val _tmpComprador: String
          _tmpComprador = _stmt.getText(_columnIndexOfComprador)
          _item = RegistroLeche(_tmpId,_tmpLitros,_tmpPrecioPorLitro,_tmpFecha,_tmpNotaVoz,_tmpComprador)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun obtenerTotalLitrosFiltrado(desde: Long): Flow<Double?> {
    val _sql: String = "SELECT SUM(litros) FROM registros_leche WHERE fecha >= ?"
    return createFlow(__db, false, arrayOf("registros_leche")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, desde)
        val _result: Double?
        if (_stmt.step()) {
          val _tmp: Double?
          if (_stmt.isNull(0)) {
            _tmp = null
          } else {
            _tmp = _stmt.getDouble(0)
          }
          _result = _tmp
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun obtenerGananciaTotalFiltrada(desde: Long): Flow<Double?> {
    val _sql: String = "SELECT SUM(litros * precioPorLitro) FROM registros_leche WHERE fecha >= ?"
    return createFlow(__db, false, arrayOf("registros_leche")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, desde)
        val _result: Double?
        if (_stmt.step()) {
          val _tmp: Double?
          if (_stmt.isNull(0)) {
            _tmp = null
          } else {
            _tmp = _stmt.getDouble(0)
          }
          _result = _tmp
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun obtenerTotalLitros(): Flow<Double?> {
    val _sql: String = "SELECT SUM(litros) FROM registros_leche"
    return createFlow(__db, false, arrayOf("registros_leche")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Double?
        if (_stmt.step()) {
          val _tmp: Double?
          if (_stmt.isNull(0)) {
            _tmp = null
          } else {
            _tmp = _stmt.getDouble(0)
          }
          _result = _tmp
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun obtenerGananciaTotal(): Flow<Double?> {
    val _sql: String = "SELECT SUM(litros * precioPorLitro) FROM registros_leche"
    return createFlow(__db, false, arrayOf("registros_leche")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Double?
        if (_stmt.step()) {
          val _tmp: Double?
          if (_stmt.isNull(0)) {
            _tmp = null
          } else {
            _tmp = _stmt.getDouble(0)
          }
          _result = _tmp
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun obtenerGananciaEntreFechas(inicio: Long, fin: Long): Double? {
    val _sql: String = "SELECT SUM(litros * precioPorLitro) FROM registros_leche WHERE fecha BETWEEN ? AND ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, inicio)
        _argIndex = 2
        _stmt.bindLong(_argIndex, fin)
        val _result: Double?
        if (_stmt.step()) {
          val _tmp: Double?
          if (_stmt.isNull(0)) {
            _tmp = null
          } else {
            _tmp = _stmt.getDouble(0)
          }
          _result = _tmp
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun obtenerLitrosEntreFechas(inicio: Long, fin: Long): Double? {
    val _sql: String = "SELECT SUM(litros) FROM registros_leche WHERE fecha BETWEEN ? AND ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, inicio)
        _argIndex = 2
        _stmt.bindLong(_argIndex, fin)
        val _result: Double?
        if (_stmt.step()) {
          val _tmp: Double?
          if (_stmt.isNull(0)) {
            _tmp = null
          } else {
            _tmp = _stmt.getDouble(0)
          }
          _result = _tmp
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun borrarTodoElHistorial() {
    val _sql: String = "DELETE FROM registros_leche"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
