package com.emiliano.lechapp

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = precioTxt,
                    onValueChange = { precioTxt = it },
                    label = { Text("Precio por Litro ($)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.guardarRegistroManual(
                            context = context,
                            litrosTxt = litrosTxt,
                            precioTxt = precioTxt,
                            compradorTxt = compradorTxt
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
    val registros by viewModel.registrosFiltrados.collectAsState()
    val totalGanancia by viewModel.gananciaTotal.collectAsState()
    val totalLitros by viewModel.totalLitros.collectAsState()
    val filtro by viewModel.filtroActual.collectAsState()

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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(100.dp), tint = MaterialTheme.colorScheme.primary)
        
        Text(perfil?.nombreGanadero ?: "Ganadero", style = MaterialTheme.typography.headlineMedium)
        Text("${perfil?.cantidadAnimales ?: 0} Animales", style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(32.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            EstadisticaSimple("Litros Totales", String.format(Locale.getDefault(), "%.1f", totalHistoricoLitros))
            EstadisticaSimple("Dinero Total", totalGananciaHistorica.formatearDinero())
        }
        
        Spacer(modifier = Modifier.height(64.dp))
        
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
