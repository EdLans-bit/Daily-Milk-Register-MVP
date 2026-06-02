package com.emiliano.lechapp

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

enum class FiltroTiempo { DIA, SEMANA, MES, TODO }

class LecheViewModel(
    
    private val dao: UsuarioDao,
    val relacionalesDao: RegistrosRelacionalesDao,
    private val geminiService: GeminiService
) : ViewModel() {

    private val _esUsuarioPremium = MutableStateFlow(false)
    val esUsuarioPremium: StateFlow<Boolean> = _esUsuarioPremium
    
    val sensibilidadAlertas = MutableStateFlow(15.0)

    fun activarModoDemoPremium() {
        _esUsuarioPremium.value = true
    }

    private val _rankingVacas = MutableStateFlow<List<RankingVaca>>(emptyList())
    val rankingVacas: StateFlow<List<RankingVaca>> = _rankingVacas

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

    private val _triggerMicrofono = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val triggerMicrofono = _triggerMicrofono.asSharedFlow()

    private val _ingresosSemanales = MutableStateFlow(0.0)
    val ingresosSemanales: StateFlow<Double> = _ingresosSemanales.asStateFlow()

    private val _gastosSemanales = MutableStateFlow(0.0)
    val gastosSemanales: StateFlow<Double> = _gastosSemanales.asStateFlow()

    private val _balanceSemanal = MutableStateFlow(0.0)
    val balanceSemanal: StateFlow<Double> = _balanceSemanal.asStateFlow()

    fun activarMicrofonoDesdeHardware() {
        _triggerMicrofono.tryEmit(Unit)
    }

    fun limpiarConsulta() { _mensajeConsulta.value = null }

    @OptIn(ExperimentalCoroutinesApi::class)
    val registrosFiltrados: StateFlow<List<RegistroConDetalles>> = _filtroActual.flatMapLatest { filtro ->
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

    fun borrarAnimal(animal: AnimalLote) {
        viewModelScope.launch(Dispatchers.IO) {
            relacionalesDao.borrarAnimal(animal)
        }
    }

    fun borrarComprador(comprador: Comprador) {
        viewModelScope.launch(Dispatchers.IO) {
            relacionalesDao.borrarComprador(comprador)
        }
    }

    fun calcularRentabilidadSemanal() {
        viewModelScope.launch(Dispatchers.IO) {
            val haceSieteDias = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
            val ingresos = relacionalesDao.calcularIngresosTotales(haceSieteDias)
            val gastos = relacionalesDao.calcularGastosTotales(haceSieteDias)
            
            _ingresosSemanales.value = ingresos
            _gastosSemanales.value = gastos
            _balanceSemanal.value = ingresos - gastos
        }
    }

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

    fun guardarRegistroManual(
        context: Context,
        litrosTxt: String,
        precioTxt: String,
        nombreAnimal: String,
        nombreComprador: String,
        fechaTxt: String
    ) {
        val litros = litrosTxt.replace(",", ".").toDoubleOrNull()
        if (litros == null || litros <= 0.0) {
            Toast.makeText(context, "Por favor ingresa una cantidad válida de litros", Toast.LENGTH_SHORT).show()
            return
        }

        val precioUI = precioTxt.replace(",", ".").toDoubleOrNull() ?: _precioPorDefecto.value
        val animalFinal = nombreAnimal.ifBlank { "General" }
        val compradorFinal = nombreComprador.ifBlank { "General" }

        val formato = SimpleDateFormat("dd / MM / yyyy", Locale.getDefault())
        val fechaMilis = try {
            formato.parse(fechaTxt)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }

        viewModelScope.launch(Dispatchers.IO) {
            val compradorExistente = relacionalesDao.findCompradorByName(compradorFinal)
            val (comprador, precioTransaccion) = if (compradorExistente != null) {
                compradorExistente to compradorExistente.precioBase
            } else {
                val nuevoC = Comprador(nombre = compradorFinal, precioBase = precioUI)
                val id = relacionalesDao.insertComprador(nuevoC)
                nuevoC.copy(idComprador = id.toInt()) to precioUI
            }

            val animalLote = relacionalesDao.findAnimalLoteByName(animalFinal)
                ?: AnimalLote(identificador = animalFinal, esLoteGeneral = true).let {
                    val id = relacionalesDao.insertAnimalLote(it)
                    it.copy(idAnimal = id.toInt())
                }

            val nuevoRegistro = RegistroLeche(
                litros = litros,
                precioPorLitro = precioTransaccion,
                compradorId = comprador.idComprador,
                animalId = animalLote.idAnimal,
                fecha = fechaMilis,
                notaVoz = "Ingreso manual"
            )
            
            dao.insertarRegistroLeche(nuevoRegistro)

            withContext(Dispatchers.Main) {
                val msg = if (compradorExistente != null) 
                    "¡Guardado! Precio usado: $$precioTransaccion (Pactado)" 
                else "¡Guardado! Nuevo comprador creado."
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
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

    suspend fun generarTextoCompartir(registros: List<RegistroConDetalles>): String = withContext(Dispatchers.Default) {
        val total = registros.sumOf { it.registro.litros }
        val porComprador = registros.groupBy { it.comprador?.nombre ?: "General" }
            .mapValues { it.value.sumOf { r -> r.registro.litros } }

        val sb = StringBuilder()
        sb.append("*Resumen Semanal de Leche*\n")
        sb.append("----------------------------\n")
        porComprador.forEach { (nombre, litros) ->
            sb.append("*$nombre:* $litros L\n")
        }
        sb.append("----------------------------\n")
        sb.append("*TOTAL:* $total L")
        sb.toString()
    }

    suspend fun generarArchivoCSV(context: Context, registros: List<RegistroConDetalles>): Uri? = withContext(Dispatchers.IO) {
        try {
            val file = File(context.cacheDir, "reporte_leche.csv")
            val writer = file.bufferedWriter()
            writer.write("Fecha,Vaca/Lote,Raza,Comprador,Litros,Precio,Total\n")
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            registros.forEach { item ->
                val r = item.registro
                val fecha = sdf.format(Date(r.fecha))
                val animal = item.animalLote?.identificador ?: "General"
                val raza = item.animalLote?.raza ?: "N/A"
                val comprador = item.comprador?.nombre ?: "General"
                val total = r.litros * r.precioPorLitro
                writer.write("$fecha,$animal,$raza,$comprador,${r.litros},${r.precioPorLitro},$total\n")
            }
            writer.close()
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun generarArchivoPDF(context: Context, registros: List<RegistroConDetalles>): Uri? = withContext(Dispatchers.IO) {
        try {
            val pdfDocument = android.graphics.pdf.PdfDocument()
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            val paint = android.graphics.Paint()

            paint.textSize = 20f
            paint.isFakeBoldText = true
            canvas.drawText("Reporte de Producción Lechera", 50f, 50f, paint)

            paint.textSize = 12f
            paint.isFakeBoldText = false
            val fechaActual = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            canvas.drawText("Fecha: $fechaActual", 50f, 80f, paint)

            var y = 120f
            paint.isFakeBoldText = true
            canvas.drawText("Fecha", 50f, y, paint)
            canvas.drawText("Animal", 130f, y, paint)
            canvas.drawText("Comprador", 250f, y, paint)
            canvas.drawText("Litros", 400f, y, paint)
            canvas.drawText("Total", 480f, y, paint)

            canvas.drawLine(50f, y + 5, 550f, y + 5, paint)
            y += 30f

            paint.isFakeBoldText = false
            val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
            registros.forEach { item ->
                if (y > 800) return@forEach
                val r = item.registro
                canvas.drawText(sdf.format(Date(r.fecha)), 50f, y, paint)
                canvas.drawText(item.animalLote?.identificador ?: "Gral", 130f, y, paint)
                canvas.drawText(item.comprador?.nombre ?: "Gral", 250f, y, paint)
                canvas.drawText("${r.litros}", 400f, y, paint)
                canvas.drawText("${(r.litros * r.precioPorLitro).toInt()}", 480f, y, paint)
                y += 25f
            }

            pdfDocument.finishPage(page)
            val file = File(context.cacheDir, "reporte_leche.pdf")
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            null
        }
    }

    private val _estadoPredictivo = MutableStateFlow<ResultadoPredictivo?>(null)
    val estadoPredictivo: StateFlow<ResultadoPredictivo?> = _estadoPredictivo

    fun generarAnalisis(registros: List<RegistroLeche>?) {
        if (registros.isNullOrEmpty()) return

        val produccionActual = registros.last().litros
        val produccionAnterior = if (registros.size > 1) registros[registros.size - 2].litros else produccionActual

        val variacion = produccionActual - produccionAnterior
        val porcentajeCaida = if (produccionAnterior > 0) (variacion / produccionAnterior) * 100 else 0.0
        val promedioSemanal = registros.takeLast(7).map { it.litros }.average()

        val prediccionSimple = (produccionActual + promedioSemanal) / 2

        if (_esUsuarioPremium.value) {
            val insight = if (variacion < 0) {
                "La producción actual está un ${String.format("%.1f", Math.abs(porcentajeCaida))}% por debajo de la entrega anterior. Promedio: ${String.format("%.1f", promedioSemanal)}L."
            } else {
                "Rendimiento superior al promedio semanal. Estabilidad detectada."
            }

            val alerta = if (variacion < 0 && produccionActual < promedioSemanal) {
                AlertaGenerada(NivelAlerta.PRECAUCION, "Se espera una caída continua en los próximos días.")
            } else null

            _estadoPredictivo.value = ResultadoPredictivo(
                prediccion = prediccionSimple,
                insight = insight,
                alerta = alerta,
                litrosPredichos = prediccionSimple,
                insightTexto = insight,
                porcentajeCaida = abs(porcentajeCaida)
            )
        } else {
            val insight = if (variacion < 0) "La producción está bajando"
            else if (variacion > 0) "La producción está subiendo"
            else "La producción está estable"

            val alerta = if (porcentajeCaida <= -sensibilidadAlertas.value) {
                AlertaGenerada(NivelAlerta.CRITICO, "La producción bajó un ${String.format("%.1f", Math.abs(porcentajeCaida))}%")
            } else null

            _estadoPredictivo.value = ResultadoPredictivo(
                prediccion = prediccionSimple,
                insight = insight,
                alerta = alerta,
                litrosPredichos = prediccionSimple,
                insightTexto = insight,
                porcentajeCaida = abs(porcentajeCaida)
            )
        }
    }

    fun generarAnalisis(animalId: Int) {
        val registrosTotales = registrosFiltrados.value

        if (registrosTotales.isEmpty()) {
            _estadoPredictivo.value = null
            return
        }

        val registrosVaca = registrosTotales.filter { it.registro.animalId == animalId }
        val historialLitros = registrosVaca.map { it.registro.litros }

        val resultado = analizarProduccion(historialLitros)
        _estadoPredictivo.value = resultado
    }

    private fun analizarProduccion(historial: List<Double>): ResultadoPredictivo {
        if (historial.size < 3) {
            return ResultadoPredictivo(0.0, "Datos insuficientes para análisis", null)
        }

        val produccionActual = historial.last()
        val produccionAnterior = historial[historial.size - 2]

        val variacion = if (produccionAnterior > 0) {
            ((produccionActual - produccionAnterior) / produccionAnterior) * 100
        } else 0.0

        val periodo = minOf(7, historial.size)
        val alfa = 2.0 / (periodo + 1)

        var ema = historial.first()
        for (i in 1 until historial.size) {
            ema = (historial[i] * alfa) + (ema * (1 - alfa))
        }

        val alerta = when {
            variacion <= -15.0 -> AlertaGenerada(NivelAlerta.CRITICO, "Caída crítica del ${String.format("%.1f", Math.abs(variacion))}% en producción.")
            variacion <= -5.0 -> AlertaGenerada(NivelAlerta.PRECAUCION, "Descenso leve de producción detectado.")
            else -> null
        }

        val insightIndividual = if (variacion < 0) "Tendencia a la baja" else "Tendencia estable o al alza"

        return ResultadoPredictivo(
            prediccion = ema,
            insight = insightIndividual,
            alerta = alerta,
            litrosPredichos = ema,
            insightTexto = insightIndividual,
            porcentajeCaida = abs(variacion)
        )
    }
}
