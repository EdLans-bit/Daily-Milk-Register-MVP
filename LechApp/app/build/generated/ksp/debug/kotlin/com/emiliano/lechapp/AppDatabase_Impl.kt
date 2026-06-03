package com.emiliano.lechapp

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class AppDatabase_Impl : AppDatabase() {
  private val _usuarioDao: Lazy<UsuarioDao> = lazy {
    UsuarioDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(3, "cc31fb79951e4aa8f0f4a5f9fc5fe72e", "592352f9df7029b4d2f5219defc2d62f") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `perfil_usuario` (`id` INTEGER NOT NULL, `nombreGanadero` TEXT NOT NULL, `cantidadAnimales` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `registros_leche` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `litros` REAL NOT NULL, `precioPorLitro` REAL NOT NULL, `fecha` INTEGER NOT NULL, `notaVoz` TEXT, `comprador` TEXT NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'cc31fb79951e4aa8f0f4a5f9fc5fe72e')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `perfil_usuario`")
        connection.execSQL("DROP TABLE IF EXISTS `registros_leche`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection): RoomOpenDelegate.ValidationResult {
        val _columnsPerfilUsuario: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsPerfilUsuario.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPerfilUsuario.put("nombreGanadero", TableInfo.Column("nombreGanadero", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPerfilUsuario.put("cantidadAnimales", TableInfo.Column("cantidadAnimales", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysPerfilUsuario: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesPerfilUsuario: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoPerfilUsuario: TableInfo = TableInfo("perfil_usuario", _columnsPerfilUsuario, _foreignKeysPerfilUsuario, _indicesPerfilUsuario)
        val _existingPerfilUsuario: TableInfo = read(connection, "perfil_usuario")
        if (!_infoPerfilUsuario.equals(_existingPerfilUsuario)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |perfil_usuario(com.emiliano.lechapp.PerfilUsuario).
              | Expected:
              |""".trimMargin() + _infoPerfilUsuario + """
              |
              | Found:
              |""".trimMargin() + _existingPerfilUsuario)
        }
        val _columnsRegistrosLeche: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsRegistrosLeche.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRegistrosLeche.put("litros", TableInfo.Column("litros", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRegistrosLeche.put("precioPorLitro", TableInfo.Column("precioPorLitro", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRegistrosLeche.put("fecha", TableInfo.Column("fecha", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRegistrosLeche.put("notaVoz", TableInfo.Column("notaVoz", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRegistrosLeche.put("comprador", TableInfo.Column("comprador", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysRegistrosLeche: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesRegistrosLeche: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoRegistrosLeche: TableInfo = TableInfo("registros_leche", _columnsRegistrosLeche, _foreignKeysRegistrosLeche, _indicesRegistrosLeche)
        val _existingRegistrosLeche: TableInfo = read(connection, "registros_leche")
        if (!_infoRegistrosLeche.equals(_existingRegistrosLeche)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |registros_leche(com.emiliano.lechapp.RegistroLeche).
              | Expected:
              |""".trimMargin() + _infoRegistrosLeche + """
              |
              | Found:
              |""".trimMargin() + _existingRegistrosLeche)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "perfil_usuario", "registros_leche")
  }

  public override fun clearAllTables() {
    super.performClear(false, "perfil_usuario", "registros_leche")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(UsuarioDao::class, UsuarioDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>): List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun usuarioDao(): UsuarioDao = _usuarioDao.value
}
