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
    private val geminiService: GeminiService,
) : ViewModel() {

    class Factory(private val dao: UsuarioDao, private val geminiService: GeminiService) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LecheViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return LecheViewModel(dao, geminiService) as T
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
    val registrosFiltrados: StateFlow<List<RegistroLeche>> = _filtroActual.flatMapLatest { filtro ->
        if (filtro == FiltroTiempo.TODO) dao.obtenerTodosLosRegistros()
        else dao.obtenerRegistrosFiltrados(calcularMilisegundosDesde(filtro))
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
        
        // Validación de litros
        if (litros == null || litros <= 0.0) {
            Toast.makeText(context, "Por favor ingresa una cantidad válida de litros", Toast.LENGTH_SHORT).show()
            return
        }

        // Precio por defecto si falla o está vacío
        val precio = precioTxt.replace(",", ".").toDoubleOrNull() ?: _precioPorDefecto.value
        
        // Comprador por defecto si está vacío
        val comprador = if (compradorTxt.isBlank()) "General" else compradorTxt

        // Convertimos el texto de la fecha ("dd / MM / yyyy") a milisegundos (Long)
        val formato = java.text.SimpleDateFormat("dd / MM / yyyy", java.util.Locale.getDefault())
        val fechaMilis = try {
            formato.parse(fechaTxt)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis() // Si algo falla, toma la fecha y hora actual
        }

        viewModelScope.launch(Dispatchers.IO) {
            val nuevoRegistro = RegistroLeche(
                litros = litros,
                precioPorLitro = precio,
                comprador = comprador,
                fecha = fechaMilis,
                notaVoz = "Ingreso manual"
            )
            
            dao.insertarRegistroLeche(nuevoRegistro)

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "¡Guardado manual! $litros L para $comprador", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun procesarEntradaVoz(context: Context, textoVoz: String) {
        viewModelScope.launch {
            if (NetworkUtils.estaOnline(context)) {
                val respuestaIA = geminiService.procesarVozConIA(textoVoz)
                if (respuestaIA != null && respuestaIA.contains("[")) {
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
            val array = JSONArray(jsonStr.replace("```json", "").replace("```", "").trim())
            for (i in 0 until array.length()) {
                val json = array.getJSONObject(i)
                if (json.optString("intencion") == "consulta") {
                    realizarConsulta(json.getString("fechaInicio"), json.getString("fechaFin"))
                } else {
                    val r = RegistroLeche(
                        litros = json.optDouble("litros", 0.0),
                        precioPorLitro = json.optDouble("precio", _precioPorDefecto.value),
                        comprador = normalizarConFuzzy(json.optString("comprador", "General")),
                        notaVoz = original,
                        fecha = parsearFechaIA(json.optString("fecha"))
                    )
                    if (r.litros > 0) dao.insertarRegistroLeche(r)
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
                val r = resultado.registro
                val registroNormalizado = r.copy(comprador = normalizarConFuzzy(r.comprador))
                withContext(Dispatchers.IO) { dao.insertarRegistroLeche(registroNormalizado) }
                withContext(Dispatchers.Main) { 
                    Toast.makeText(context, "Local: Guardado ${r.litros}L para ${r.comprador}", Toast.LENGTH_SHORT).show() 
                }
            }
            is AccionVoz.Consulta -> {
                val ganancia = dao.obtenerGananciaEntreFechas(resultado.fechaInicio, resultado.fechaFin) ?: 0.0
                withContext(Dispatchers.Main) {
                    val mensaje = "Ganancia en el periodo: ${ganancia.formatearDinero()}"
                    _mensajeConsulta.value = mensaje // Mostramos en el AlertDialog si existe, o Toast
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

    private suspend fun normalizarConFuzzy(nombre: String): String {
        // En un caso real, obtendríamos solo los nombres únicos de la BD
        val existentes = registrosFiltrados.value.map { it.comprador }.distinct()
        return TextoUtils.encontrarMasCercano(nombre, existentes)
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

    fun agruparDatos(registros: List<RegistroLeche>, filtro: FiltroTiempo): Map<String, Double> {
        val registrosOrdenados = registros.sortedBy { it.fecha }
        return when (filtro) {
            FiltroTiempo.DIA -> {
                registrosOrdenados.groupBy { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it.fecha)) }
                    .mapValues { it.value.sumOf { r -> r.litros } }
            }
            FiltroTiempo.SEMANA -> {
                registrosOrdenados.groupBy { SimpleDateFormat("EEE", Locale.getDefault()).format(Date(it.fecha)) }
                    .mapValues { it.value.sumOf { r -> r.litros } }
            }
            FiltroTiempo.MES -> {
                // Agrupar por semanas del mes: "Sem 1", "Sem 2", etc.
                registrosOrdenados.groupBy {
                    val cal = Calendar.getInstance().apply { timeInMillis = it.fecha }
                    "Sem ${cal.get(Calendar.WEEK_OF_MONTH)}"
                }.mapValues { it.value.sumOf { r -> r.litros } }
            }
            FiltroTiempo.TODO -> {
                registrosOrdenados.groupBy { SimpleDateFormat("MMM", Locale.getDefault()).format(Date(it.fecha)) }
                    .mapValues { it.value.sumOf { r -> r.litros } }
            }
        }
    }
}

