package com.emiliano.lechapp

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.emiliano.lechapp.ui.BotonMicrofono
import java.text.SimpleDateFormat
import java.util.*

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Registro : Screen("registro", "Registro", Icons.Default.Mic)
    object Historial : Screen("historial", "Estadísticas", Icons.Default.BarChart)
    object Perfil : Screen("perfil", "Perfil", Icons.Default.Person)
}

@Composable
fun NavegacionPrincipal(viewModel: LecheViewModel) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Registro) }
    val mensajeConsulta by viewModel.mensajeConsulta.collectAsState()

    if (mensajeConsulta != null) {
        AlertDialog(
            onDismissRequest = { viewModel.limpiarConsulta() },
            confirmButton = { TextButton(onClick = { viewModel.limpiarConsulta() }) { Text("OK") } },
            title = { Text("Resultado de Consulta") },
            text = { Text(mensajeConsulta!!) }
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                val screens = listOf(Screen.Registro, Screen.Historial, Screen.Perfil)
                screens.forEach { screen ->
                    NavigationBarItem(
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen },
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentScreen) {
                Screen.Registro -> PantallaEntrada(viewModel)
                Screen.Historial -> PantallaEstadisticas(viewModel)
                Screen.Perfil -> PantallaPerfilDetalle(viewModel)
            }
        }
    }
}

@Composable
fun PantallaEntrada(viewModel: LecheViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val precioDefecto by viewModel.precioPorDefecto.collectAsState()

    // Estados para el registro manual
    var litrosTxt by remember { mutableStateOf("") }
    // Control del calendario
    var showDatePicker by remember { mutableStateOf(false) }
    var fechaSeleccionada by remember {
        mutableStateOf(java.text.SimpleDateFormat("dd / MM / yyyy", java.util.Locale.getDefault()).format(java.util.Date()))
    }
    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
    val datePickerState = androidx.compose.material3.rememberDatePickerState()
    var compradorTxt by remember { mutableStateOf("") }
    var precioTxt by remember { mutableStateOf(precioDefecto.toString()) }

    // Actualizar el campo de precio si cambia el precio por defecto globalmente
    LaunchedEffect(precioDefecto) {
        if (precioTxt.isEmpty() || precioTxt == "0.0") {
            precioTxt = precioDefecto.toString()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            "Nuevo Registro",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        // Panel de Registro Manual
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Ingreso Manual",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // --- INICIO DEL CALENDARIO ---
                @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
                if (showDatePicker) {
                    androidx.compose.material3.DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            androidx.compose.material3.TextButton(onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    val formatter = java.text.SimpleDateFormat("dd / MM / yyyy", java.util.Locale.getDefault())
                                    fechaSeleccionada = formatter.format(java.util.Date(millis))
                                }
                                showDatePicker = false
                            }) { Text("Aceptar") }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
                        }
                    ) {
                        androidx.compose.material3.DatePicker(state = datePickerState)
                    }
                }

                OutlinedTextField(
                    value = fechaSeleccionada,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fecha de Ordeño / Venta") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        androidx.compose.material3.IconButton(onClick = { showDatePicker = true }) {
                            androidx.compose.material3.Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.DateRange,
                                contentDescription = "Abrir Calendario"
                            )
                        }
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))
                // --- FIN DEL CALENDARIO ---

                OutlinedTextField(
                    value = litrosTxt,
                    onValueChange = { litrosTxt = it },
                    label = { Text("Litros") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = compradorTxt,
                    onValueChange = { compradorTxt = it },
                    label = { Text("Comprador") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.guardarRegistroManual(
                            context = context,
                            litrosTxt = litrosTxt,
                            precioTxt = "",
                            compradorTxt = compradorTxt,
                            fechaTxt = fechaSeleccionada
                        )
                        // Limpiar campos después de guardar exitosamente
                        litrosTxt = ""
                        compradorTxt = ""

                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Guardar Registro")
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "O usa el Micrófono",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        BotonMicrofono(
            onTextoEscuchado = { viewModel.procesarEntradaVoz(context, it) },
            modifier = Modifier.size(100.dp),
            externalTrigger = viewModel.triggerMicrofono
        )
        
        Text(
            "Di 'Vendí 20 l a Juan' o 'Ayer vendí 15'",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PantallaEstadisticas(viewModel: LecheViewModel) {
    var periodoSeleccionado by remember {mutableStateOf(FiltroTiempo.SEMANA)}
    val registros by viewModel.registrosFiltrados.collectAsState(initial = emptyList())
    val totalGanancia by viewModel.gananciaTotal.collectAsState(initial = 0.0)
    val totalLitros by viewModel.totalLitros.collectAsState(initial = 0.0)
    val filtro by viewModel.filtroActual.collectAsState(initial = FiltroTiempo.SEMANA)
    val datosAgrupados = remember(registros, filtro) {
        viewModel.agruparDatos(registros, filtro)
    }

    val registrosPorFecha = remember(registros) {
        registros.groupBy { formatTimestampToDate(it.fecha) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Ganancia Total (${filtro.name})", style = MaterialTheme.typography.labelLarge)
                Text(totalGanancia.formatearDinero(), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text("${String.format(Locale.getDefault(), "%.1f", totalLitros)} Litros totales", style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (datosAgrupados.isNotEmpty()) {
            GraficoBarras(datosAgrupados)
        } else {
            Text("No hay datos para este periodo", modifier = Modifier.padding(16.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FiltroTiempo.values().forEach { f ->
                FilterChip(
                    selected = filtro == f,
                    onClick = { viewModel.cambiarFiltro(f) },
                    label = { Text(f.name) }
                )
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
            registrosPorFecha.forEach { (fecha, itemsDeEseDia) ->
                stickyHeader {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = fecha,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                items(itemsDeEseDia) { registro ->
                    RegistroItemConBorrado(registro, onBorrar = { viewModel.borrarRegistro(it) })
                }
            }
        }
    }
}

private fun formatTimestampToDate(timestamp: Long): String {
    val cal = Calendar.getInstance()
    val hoy = Calendar.getInstance()
    cal.timeInMillis = timestamp

    return when {
        isSameDay(cal, hoy) -> "Hoy"
        isSameDay(cal, Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }) -> "Ayer"
        else -> SimpleDateFormat("dd 'de' MMMM", Locale("es", "ES")).format(Date(timestamp))
    }
}

private fun isSameDay(c1: Calendar, c2: Calendar): Boolean {
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
           c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}

@Composable
fun RegistroItemConBorrado(registro: RegistroLeche, onBorrar: (RegistroLeche) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("${registro.litros} L", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Comprador: ${registro.comprador}", style = MaterialTheme.typography.bodySmall)
                Text(
                    text = "Total: ${(registro.litros * registro.precioPorLitro).formatearDinero()}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            IconButton(onClick = { onBorrar(registro) }) {
                Icon(Icons.Default.Delete, contentDescription = "Borrar", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun PantallaPerfilDetalle(viewModel: LecheViewModel) {
    val perfil by viewModel.perfilUsuario.collectAsState()
    val totalHistoricoLitros by viewModel.totalLitros.collectAsState()
    val totalGananciaHistorica by viewModel.gananciaTotal.collectAsState()

    // --- Variables de Memoria para el Precio Maestro ---
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPreferences = context.getSharedPreferences("PreferenciasLechApp", android.content.Context.MODE_PRIVATE)
    val precioGuardado = sharedPreferences.getFloat("PRECIO_BASE", 0f)
    var precioTxt by remember { mutableStateOf(if (precioGuardado > 0) precioGuardado.toString() else "") }

    // Agregamos scroll vertical por si la pantalla es pequeña
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(androidx.compose.foundation.rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Cabecera del Perfil (Tu código original)
        Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(100.dp), tint = MaterialTheme.colorScheme.primary)

        Text(perfil?.nombreGanadero ?: "Ganadero", style = MaterialTheme.typography.headlineMedium)
        Text("${perfil?.cantidadAnimales ?: 0} Animales", style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(32.dp))

        // 2. Estadísticas Históricas (Tu código original)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            EstadisticaSimple("Litros Totales", String.format(java.util.Locale.getDefault(), "%.1f", totalHistoricoLitros))
            EstadisticaSimple("Dinero Total", totalGananciaHistorica.formatearDinero())
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 3. Tarjeta de Configuración de Precio (Lo Nuevo)
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Configuración de Registro",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    "Define el precio base por litro. Se usará automáticamente en todos tus nuevos registros diarios.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.ui.graphics.Color.Gray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = precioTxt,
                    onValueChange = { precioTxt = it },
                    label = { Text("Precio por Litro ($)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    leadingIcon = { Text("$", modifier = Modifier.padding(start = 16.dp)) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val nuevoPrecio = precioTxt.replace(",", ".").toFloatOrNull()
                        if (nuevoPrecio != null && nuevoPrecio > 0) {
                            sharedPreferences.edit().putFloat("PRECIO_BASE", nuevoPrecio).apply()
                            android.widget.Toast.makeText(context, "Precio maestro actualizado a $$nuevoPrecio", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            android.widget.Toast.makeText(context, "Ingresa un precio válido", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Guardar Precio Maestro")
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { viewModel.borrarTodoElHistorial() },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Borrar Todo el Historial")
        }
    }
}
@Composable
fun EstadisticaSimple(label: String, valor: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(valor, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun PantallaPerfil(context: android.content.Context) {
    // 1. Instanciamos la memoria de SharedPreferences
    val sharedPreferences = context.getSharedPreferences("PreferenciasLechApp", android.content.Context.MODE_PRIVATE)

    // 2. Leemos el precio guardado (por defecto 0.0) para mostrarlo al entrar
    val precioGuardado = sharedPreferences.getFloat("PRECIO_BASE", 0f)
    var precioTxt by remember { mutableStateOf(if (precioGuardado > 0) precioGuardado.toString() else "") }

    // 3. Diseño visual del Perfil
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Text(
            text = "Configuración de Perfil",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp, top = 16.dp)
        )

        androidx.compose.material3.Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Precio Maestro",
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    "Define el precio base por litro. Este valor se usará automáticamente en todos tus nuevos registros diarios.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.ui.graphics.Color.Gray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                androidx.compose.material3.OutlinedTextField(
                    value = precioTxt,
                    onValueChange = { precioTxt = it },
                    label = { Text("Precio por Litro ($)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    singleLine = true,
                    leadingIcon = { Text("$", modifier = Modifier.padding(start = 16.dp)) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                androidx.compose.material3.Button(
                    onClick = {

                        val nuevoPrecio = precioTxt.replace(",", ".").toFloatOrNull()
                        if (nuevoPrecio != null && nuevoPrecio > 0) {
                            sharedPreferences.edit().putFloat("PRECIO_BASE", nuevoPrecio).apply()
                            android.widget.Toast.makeText(context, "Precio base actualizado a $$nuevoPrecio", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            android.widget.Toast.makeText(context, "Ingresa un precio válido", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Guardar Configuración")
                }
            }
        }
    }
}

@Composable
fun GraficoBarras(datos: Map<String, Double>) {
    val valores = datos.values.toList()
    val maximo = valores.maxOrNull() ?: 1.0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        datos.forEach { (etiqueta, valor) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .height((120 * (valor / maximo)).dp)
                        .width(30.dp)
                        .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(4.dp))
                )
                Text(text = etiqueta.take(3), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
