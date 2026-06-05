package com.emiliano.lechapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.emiliano.lechapp.databinding.FragmentEstadisticasBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EstadisticasFragment : Fragment() {

    private var _binding: FragmentEstadisticasBinding? = null
    private val binding get() = _binding!!

    private val lecheViewModel: LecheViewModel by activityViewModels {
        val database = AppDatabase.getDatabase(requireContext())
        LecheViewModel.Factory(
            database.usuarioDao(),
            database.registrosRelacionalesDao(),
            GeminiService()
        )
    }

    private val adapter = EntregaAdapter { registro ->
        lecheViewModel.borrarRegistro(registro)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEstadisticasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvUltimasEntregas.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        binding.rvUltimasEntregas.adapter = adapter

        // Click listeners para los filtros
        binding.btnDia.setOnClickListener { selectFilter(binding.btnDia, FiltroTiempo.DIA) }
        binding.btnSem.setOnClickListener { selectFilter(binding.btnSem, FiltroTiempo.SEMANA) }
        binding.btnMes.setOnClickListener { selectFilter(binding.btnMes, FiltroTiempo.MES) }
        binding.btnAnio.setOnClickListener { selectFilter(binding.btnAnio, FiltroTiempo.TODO) }

        binding.btnFiltroPersonalizado.setOnClickListener {
            mostrarDialogoComparacion()
        }

        binding.btnExportPdf.setOnClickListener {
            lifecycleScope.launch {
                val uri = lecheViewModel.generarArchivoPDF(requireContext(), lecheViewModel.registrosFiltrados.value)
                if (uri != null) compartirArchivo(uri)
                else Toast.makeText(requireContext(), "Error al generar PDF", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnExportExcel.setOnClickListener {
            lifecycleScope.launch {
                val uri = lecheViewModel.generarArchivoCSV(requireContext(), lecheViewModel.registrosFiltrados.value)
                if (uri != null) compartirArchivo(uri)
                else Toast.makeText(requireContext(), "Error al generar Excel", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnShare.setOnClickListener {
            lifecycleScope.launch {
                val texto = lecheViewModel.generarTextoCompartir(lecheViewModel.registrosFiltrados.value)
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT, texto)
                }
                startActivity(android.content.Intent.createChooser(intent, "Compartir reporte"))
            }
        }

        // Observar cambios en el ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    lecheViewModel.registrosFiltrados.collectLatest { registros ->
                        adapter.submitList(registros)
                        actualizarGrafica(registros, lecheViewModel.filtroActual.value)
                        
                        // Actualizar resumen del periodo (Litros y Dinero)
                        if (registros.isNotEmpty()) {
                            val totalLitros = registros.sumOf { it.registro.litros }
                            val totalDinero = registros.sumOf { it.registro.litros * it.registro.precioPorLitro }
                            binding.tvResumenPeriodo.text = String.format(Locale.getDefault(), "Total: %.1f L | $ %.0f", totalLitros, totalDinero)
                        } else {
                            binding.tvResumenPeriodo.text = "Sin datos en este periodo"
                        }
                    }
                }
                
                launch {
                    lecheViewModel.filtroActual.collectLatest { filtro ->
                        // Actualizar UI de botones según el filtro
                        updateFilterButtonsUI(filtro)
                    }
                }

                launch {
                    lecheViewModel.esUsuarioPremium.collectLatest { esPremium ->
                        binding.capaBloqueoEstadisticas.visibility = if (esPremium) View.GONE else View.VISIBLE
                    }
                }
            }
        }
    }

    private fun selectFilter(textView: TextView, filtro: FiltroTiempo) {
        lecheViewModel.cambiarFiltro(filtro)
    }

    private fun compartirArchivo(uri: android.net.Uri) {
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(android.content.Intent.createChooser(intent, "Compartir archivo"))
    }

    private fun mostrarDialogoComparacion() {
        val calendar = Calendar.getInstance()
        
        android.app.DatePickerDialog(
            requireContext(),
            { _, year1, month1, day1 ->
                val cal1 = Calendar.getInstance()
                cal1.set(year1, month1, day1)
                
                android.app.DatePickerDialog(
                    requireContext(),
                    { _, year2, month2, day2 ->
                        val cal2 = Calendar.getInstance()
                        cal2.set(year2, month2, day2)
                        
                        lecheViewModel.compararFechas(cal1.timeInMillis, cal2.timeInMillis)
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).apply {
                    setTitle("Selecciona la segunda fecha")
                    show()
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setTitle("Selecciona la primera fecha")
            show()
        }
    }

    private fun updateFilterButtonsUI(activeFilter: FiltroTiempo) {
        val activeColor = android.graphics.Color.parseColor("#1B5E20")
        val inactiveColor = android.graphics.Color.parseColor("#757575")

        binding.btnDia.setTextColor(if (activeFilter == FiltroTiempo.DIA) activeColor else inactiveColor)
        binding.btnSem.setTextColor(if (activeFilter == FiltroTiempo.SEMANA) activeColor else inactiveColor)
        binding.btnMes.setTextColor(if (activeFilter == FiltroTiempo.MES) activeColor else inactiveColor)
        binding.btnAnio.setTextColor(if (activeFilter == FiltroTiempo.TODO) activeColor else inactiveColor)
        
        // Estilo para el botón de comparación
        if (activeFilter == FiltroTiempo.COMPARAR) {
            binding.btnFiltroPersonalizado.setBackgroundColor(android.graphics.Color.parseColor("#E8F5E9"))
        } else {
            binding.btnFiltroPersonalizado.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }
    }

    private fun actualizarGrafica(registros: List<RegistroConDetalles>, filtro: FiltroTiempo) {
        val datos = lecheViewModel.agruparDatos(registros, filtro)
        
        val composeView = ComposeView(requireContext()).apply {
            setContent {
                if (filtro == FiltroTiempo.COMPARAR) {
                    ComparativaView()
                } else {
                    BarChart(datos)
                }
            }
        }
        
        binding.chartContainer.removeAllViews()
        binding.chartContainer.addView(composeView)
    }

    @Composable
    fun ComparativaView() {
        val datos by lecheViewModel.datosComparativa.collectAsState()
        val fechas by lecheViewModel.fechasComparacion.collectAsState()
        
        if (datos != null && fechas != null) {
            val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
            val label1 = sdf.format(Date(fechas!!.first))
            val label2 = sdf.format(Date(fechas!!.second))
            
            val mapa = mapOf(
                label1 to datos!!.first,
                label2 to datos!!.second
            )
            BarChart(mapa)
        }
    }

    @Composable
    fun BarChart(data: Map<String, Double>) {
        val items = data.toList()
        val maxVal = (items.maxOfOrNull { it.second } ?: 1.0).coerceAtLeast(1.0)

        Canvas(modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 24.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            
            val bottomPadding = 40.dp.toPx()
            val topPadding = 30.dp.toPx()
            val availableHeight = canvasHeight - bottomPadding - topPadding
            
            val barCount = items.size.coerceAtLeast(1)
            val totalBarWidth = canvasWidth * 0.8f
            val singleBarWidth = (totalBarWidth / barCount) * 0.6f
            val spacing = (totalBarWidth / barCount) * 0.4f
            val startOffset = (canvasWidth - totalBarWidth) / 2

            val lineCount = 4
            for (i in 0..lineCount) {
                val y = topPadding + (availableHeight / lineCount) * i
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    start = Offset(0f, y),
                    end = Offset(canvasWidth, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            items.forEachIndexed { index, pair ->
                val barHeight = ((pair.second / maxVal) * availableHeight).toFloat().coerceAtLeast(10f)
                val x = startOffset + index * (singleBarWidth + spacing) + spacing / 2
                val y = canvasHeight - bottomPadding - barHeight

                drawRoundRect(
                    color = Color(0xFF4CAF50),
                    topLeft = Offset(x, y),
                    size = Size(singleBarWidth, barHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )

                drawContext.canvas.nativeCanvas.drawText(
                    "${pair.second.toInt()}L",
                    x + singleBarWidth / 2,
                    y - 8.dp.toPx(),
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.BLACK
                        textSize = 12.sp.toPx()
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )

                drawContext.canvas.nativeCanvas.drawText(
                    pair.first,
                    x + singleBarWidth / 2,
                    canvasHeight - 10.dp.toPx(),
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 11.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
