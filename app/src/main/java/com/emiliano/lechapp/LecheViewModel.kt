package com.emiliano.lechapp

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*
import java.util.Date
import java.util.Locale

enum class FiltroTiempo { DIA, SEMANA, MES, TODO }

class LecheViewModel(
    private val dao: UsuarioDao,
    val relacionalesDao: RegistrosRelacionalesDao,
    private val geminiService: GeminiService,
) : ViewModel() {

    class Factory(
        private val dao: UsuarioDao,
        private val relacionalesDao: RegistrosRelacionalesDao,
        private val geminiService: GeminiService
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LecheViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return LecheViewModel(dao, relacionalesDao, geminiService) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    private val _precioPorDefecto = MutableStateFlow(2000.0)
    val precioPorDefecto: StateFlow<Double> = _precioPorDefecto.asStateFlow()

    private val _filtroActual = MutableStateFlow(FiltroTiempo.TODO)
    val filtroActual: StateFlow<FiltroTiempo> = _filtroActual.asStateFlow()

    private val _mensajeConsulta = MutableStateFlow<String?>(null)
    val mensajeConsulta: StateFlow<String?> = _mensajeConsulta.asStateFlow()

    // Evento para activar el micrófono desde hardware (botones de volumen)
    private val _triggerMicrofono = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val triggerMicrofono = _triggerMicrofono.asSharedFlow()

    fun activarMicrofonoDesdeHardware() {
        _triggerMicrofono.tryEmit(Unit)
    }

    fun limpiarConsulta() { _mensajeConsulta.value = null }

    @OptIn(ExperimentalCoroutinesApi::class)
    val registrosFiltrados: StateFlow<List<RegistroConDetalles>> = _filtroActual.flatMapLatest { filtro ->
        // Por ahora usamos la consulta de todos los registros con detalles
        // En una app real filtraríamos por fecha también aquí
        dao.obtenerRegistrosConDetalles()
    }.map { registros ->
        val filtro = _filtroActual.value
        if (filtro == FiltroTiempo.TODO) registros
        else {
            val desde = calcularMilisegundosDesde(filtro)
            registros.filter { it.registro.fecha >= desde }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val totalLitros: StateFlow<Double> = _filtroActual.flatMapLatest { filtro ->
        if (filtro == FiltroTiempo.TODO) dao.obtenerTotalLitros()
        else dao.obtenerTotalLitrosFiltrado(calcularMilisegundosDesde(filtro))
    }.map { it ?: 0.0 }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val gananciaTotal: StateFlow<Double> = _filtroActual.flatMapLatest { filtro ->
        if (filtro == FiltroTiempo.TODO) dao.obtenerGananciaTotal()
        else dao.obtenerGananciaTotalFiltrada(calcularMilisegundosDesde(filtro))
    }.map { it ?: 0.0 }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val perfilUsuario = flow { emit(dao.obtenerPerfil()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun actualizarPrecioPorDefecto(nuevoPrecio: Double) { _precioPorDefecto.value = nuevoPrecio }
    fun cambiarFiltro(nuevoFiltro: FiltroTiempo) { _filtroActual.value = nuevoFiltro }
    fun borrarRegistro(registro: RegistroLeche) { viewModelScope.launch(Dispatchers.IO) { dao.borrarRegistro(registro) } }
    fun borrarTodoElHistorial() { viewModelScope.launch(Dispatchers.IO) { dao.borrarTodoElHistorial() } }

    /**
     * Analiza la salud de un animal basándose en la caída de producción.
     */
    fun analizarSaludAnimal(animalId: Int, onResult: (String, List<RegistroLeche>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val haceSieteDias = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
            val registros = relacionalesDao.obtenerRegistrosRecientesAnimal(animalId, haceSieteDias)

            val resultado = when {
                registros.isEmpty() -> "Sin registros"
                registros.size < 3 -> "Insuficientes datos"
                else -> {
                    val hoy = registros.first().litros
                    val anteriores = registros.drop(1)
                    val promedioAnterior = anteriores.map { it.litros }.average()

                    val umbralAlerta = promedioAnterior * 0.85

                    if (hoy < umbralAlerta) {
                        val porcentajeCaida = ((promedioAnterior - hoy) / promedioAnterior) * 100
                        String.format(Locale.getDefault(), "Alerta: Caída de producción del %.1f%%", porcentajeCaida)
                    } else {
                        "Saludable"
                    }
                }
            }

            withContext(Dispatchers.Main) {
                onResult(resultado, registros)
            }
        }
    }

    /**
     * Guarda un registro ingresado manualmente desde los campos de texto.
     */
    fun guardarRegistroManual(
        context: Context,
        litrosTxt: String,
        precioTxt: String,
        compradorTxt: String,
        fechaTxt: String
    ) {
        val litros = litrosTxt.replace(",", ".").toDoubleOrNull()
        
        if (litros == null || litros <= 0.0) {
            Toast.makeText(context, "Por favor ingresa una cantidad válida de litros", Toast.LENGTH_SHORT).show()
            return
        }

        val precio = precioTxt.replace(",", ".").toDoubleOrNull() ?: _precioPorDefecto.value
        val nombreComprador = if (compradorTxt.isBlank()) "General" else compradorTxt

        val formato = SimpleDateFormat("dd / MM / yyyy", Locale.getDefault())
        val fechaMilis = try {
            formato.parse(fechaTxt)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }

        viewModelScope.launch(Dispatchers.IO) {
            val comprador = relacionalesDao.findCompradorByName(nombreComprador)
                ?: Comprador(nombre = nombreComprador, precioBase = 0.0).let {
                    val id = relacionalesDao.insertComprador(it)
                    it.copy(idComprador = id.toInt())
                }

            val nuevoRegistro = RegistroLeche(
                litros = litros,
                precioPorLitro = precio,
                compradorId = comprador.idComprador,
                fecha = fechaMilis,
                notaVoz = "Ingreso manual"
            )
            
            dao.insertarRegistroLeche(nuevoRegistro)

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "¡Guardado manual! $litros L para $nombreComprador", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun procesarEntradaVoz(context: Context, textoVoz: String) {
        viewModelScope.launch {
            if (NetworkUtils.estaOnline(context)) {
                val respuestaIA = geminiService.procesarVozConIA(textoVoz)
                if (respuestaIA != null && (respuestaIA.contains("[") || respuestaIA.contains("{"))) {
                    procesarJsonIA(context, respuestaIA, textoVoz)
                } else {
                    ejecutarProcesamientoLocal(context, textoVoz)
                }
            } else {
                ejecutarProcesamientoLocal(context, textoVoz)
            }
        }
    }

    private suspend fun procesarJsonIA(context: Context, jsonStr: String, original: String) {
        try {
            val cleanJson = jsonStr.replace("```json", "").replace("```", "").trim()
            val array = if (cleanJson.startsWith("[")) JSONArray(cleanJson) else JSONArray().put(org.json.JSONObject(cleanJson))
            
            for (i in 0 until array.length()) {
                val json = array.getJSONObject(i)
                val intencion = json.optString("intencion")
                
                if (intencion == "consulta") {
                    realizarConsulta(json.getString("fechaInicio"), json.getString("fechaFin"))
                } else if (intencion == "registro_leche" || intencion == "registro") {
                    val litros = json.optDouble("litros", 0.0)
                    if (litros <= 0) continue

                    val nombreComprador = json.optString("comprador", "General")
                    val nombreAnimal = json.optString("identificador_animal", "General")
                    val esLote = json.optBoolean("es_lote", false)
                    val precioIA = if (json.has("precio")) json.getDouble("precio") else _precioPorDefecto.value
                    val fechaIA = parsearFechaIA(json.optString("fecha"))

                    withContext(Dispatchers.IO) {
                        val comprador = relacionalesDao.findCompradorByName(nombreComprador)
                            ?: Comprador(nombre = nombreComprador, precioBase = 0.0).let {
                                val id = relacionalesDao.insertComprador(it)
                                it.copy(idComprador = id.toInt())
                            }

                        val animalLote = relacionalesDao.findAnimalLoteByName(nombreAnimal)
                            ?: AnimalLote(identificador = nombreAnimal, esLoteGeneral = esLote).let {
                                val id = relacionalesDao.insertAnimalLote(it)
                                it.copy(idAnimal = id.toInt())
                            }

                        val nuevoRegistro = RegistroLeche(
                            litros = litros,
                            precioPorLitro = precioIA,
                            fecha = fechaIA,
                            notaVoz = original,
                            compradorId = comprador.idComprador,
                            animalId = animalLote.idAnimal
                        )
                        dao.insertarRegistroLeche(nuevoRegistro)

                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "IA: Registro guardado (${litros}L)", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            ejecutarProcesamientoLocal(context, original)
        }
    }

    private suspend fun ejecutarProcesamientoLocal(context: Context, texto: String) {
        val procesador = ProcesadorVozLocal(_precioPorDefecto.value)
        val resultado = procesador.clasificarIntencion(texto)

        when (resultado) {
            is AccionVoz.Registro -> {
                withContext(Dispatchers.IO) {
                    val comprador = relacionalesDao.findCompradorByName(resultado.nombreComprador)
                        ?: Comprador(nombre = resultado.nombreComprador, precioBase = 0.0).let {
                            val id = relacionalesDao.insertComprador(it)
                            it.copy(idComprador = id.toInt())
                        }

                    val nuevoRegistro = RegistroLeche(
                        litros = resultado.litros,
                        precioPorLitro = resultado.precio,
                        compradorId = comprador.idComprador,
                        fecha = resultado.fecha,
                        notaVoz = resultado.notaVoz
                    )
                    dao.insertarRegistroLeche(nuevoRegistro)
                    
                    withContext(Dispatchers.Main) { 
                        Toast.makeText(context, "Local: Guardado ${resultado.litros}L para ${resultado.nombreComprador}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            is AccionVoz.Consulta -> {
                val ganancia = dao.obtenerGananciaEntreFechas(resultado.fechaInicio, resultado.fechaFin) ?: 0.0
                withContext(Dispatchers.Main) {
                    val mensaje = "Ganancia en el periodo: ${ganancia.formatearDinero()}"
                    _mensajeConsulta.value = mensaje
                    Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
                }
            }
            is AccionVoz.NoEntendido -> {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "No entendí: '$texto'", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun parsearFechaIA(fechaStr: String?): Long {
        if (fechaStr.isNullOrEmpty()) return System.currentTimeMillis()
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(fechaStr)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) { System.currentTimeMillis() }
    }

    private suspend fun realizarConsulta(inicioStr: String, finStr: String) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val inicio = sdf.parse(inicioStr)?.time ?: return
        val fin = sdf.parse(finStr)?.time?.plus(86399999) ?: return 

        val ganancia = dao.obtenerGananciaEntreFechas(inicio, fin) ?: 0.0
        val litros = dao.obtenerLitrosEntreFechas(inicio, fin) ?: 0.0
        
        val resultado = "Entre $inicioStr y $finStr:\nTotal: ${ganancia.formatearDinero()}\nLitros: ${String.format(Locale.getDefault(), "%.1f", litros)}L"
        _mensajeConsulta.value = if (_mensajeConsulta.value == null) resultado else "${_mensajeConsulta.value}\n\n$resultado"
    }

    private fun calcularMilisegundosDesde(filtro: FiltroTiempo): Long {
        val c = Calendar.getInstance()
        when (filtro) {
            FiltroTiempo.DIA -> c.add(Calendar.DAY_OF_YEAR, -1)
            FiltroTiempo.SEMANA -> c.add(Calendar.WEEK_OF_YEAR, -1)
            FiltroTiempo.MES -> c.add(Calendar.MONTH, -1)
            FiltroTiempo.TODO -> return 0L
        }
        return c.timeInMillis
    }

    fun agruparDatos(registros: List<RegistroConDetalles>, filtro: FiltroTiempo): Map<String, Double> {
         val registrosOrdenados = registros.sortedBy { it.registro.fecha }
        return when (filtro) {
            FiltroTiempo.DIA -> {
                registrosOrdenados.groupBy { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it.registro.fecha)) }
                    .mapValues { it.value.sumOf { r -> r.registro.litros } }
            }
            FiltroTiempo.SEMANA -> {
                registrosOrdenados.groupBy { SimpleDateFormat("EEE", Locale.getDefault()).format(Date(it.registro.fecha)) }
                    .mapValues { it.value.sumOf { r -> r.registro.litros } }
            }
            FiltroTiempo.MES -> {
                registrosOrdenados.groupBy {
                    val cal = Calendar.getInstance().apply { timeInMillis = it.registro.fecha }
                    "Sem ${cal.get(Calendar.WEEK_OF_MONTH)}"
                }.mapValues { it.value.sumOf { r -> r.registro.litros } }
            }
            FiltroTiempo.TODO -> {
                registrosOrdenados.groupBy { SimpleDateFormat("MMM", Locale.getDefault()).format(Date(it.registro.fecha)) }
                    .mapValues { it.value.sumOf { r -> r.registro.litros } }
            }
        }
    }
}
