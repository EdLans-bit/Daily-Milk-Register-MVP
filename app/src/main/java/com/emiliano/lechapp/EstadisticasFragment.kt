package com.emiliano.lechapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.CornerRadius
import com.emiliano.lechapp.databinding.FragmentEstadisticasBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class EstadisticasFragment : Fragment() {

    private var _binding: FragmentEstadisticasBinding? = null
    private val binding get() = _binding!!

    private val lecheViewModel: LecheViewModel by activityViewModels {
        LecheViewModel.Factory(
            AppDatabase.getDatabase(requireContext()).usuarioDao(),
            AppDatabase.getDatabase(requireContext()).registrosRelacionalesDao(),
            GeminiService()
        )
    }
    private val adapter = EntregaAdapter { registro ->
        lecheViewModel.borrarRegistro(registro)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_estadisticas, container, false)
    }

    // 2. Paste the new onViewCreated right below it
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find all the elements by their exact XML IDs
        val btnDia = view.findViewById<Button>(R.id.btn_dia)
        val btnSem = view.findViewById<Button>(R.id.btn_sem)
        val btnMes = view.findViewById<Button>(R.id.btn_mes)
        val btnAnio = view.findViewById<Button>(R.id.btn_anio)

        val chartContainer = view.findViewById<LinearLayout>(R.id.chartContainer)

        val tvPrediccion = view.findViewById<TextView>(R.id.tvPrediccionPeriodo)
        val btnComparacion = view.findViewById<Button>(R.id.btnFiltroPersonalizado)
        val capaBloqueo = view.findViewById<LinearLayout>(R.id.capaBloqueoEstadisticas)
        val btnDesbloquear = view.findViewById<Button>(R.id.btnDesbloquearEstadisticas)

        val rvEntregas = view.findViewById<RecyclerView>(R.id.rvUltimasEntregas)

        // Click listeners here
        btnDia.setOnClickListener {
            btnDia.setTextColor(android.graphics.Color.parseColor("#1B5E20"))
            btnSem.setTextColor(android.graphics.Color.parseColor("#757575"))
            btnMes.setTextColor(android.graphics.Color.parseColor("#757575"))
            btnAnio.setTextColor(android.graphics.Color.parseColor("#757575"))

            // 2. Activar la lógica de tu ViewModel
            lecheViewModel.cambiarFiltro(FiltroTiempo.DIA)

        }
        btnSem.setOnClickListener {
            btnSem.setTextColor(android.graphics.Color.parseColor("#1B5E20"))
            btnDia.setTextColor(android.graphics.Color.parseColor("#757575"))
            btnMes.setTextColor(android.graphics.Color.parseColor("#757575"))
            btnAnio.setTextColor(android.graphics.Color.parseColor("#757575"))

            lecheViewModel.cambiarFiltro(FiltroTiempo.SEMANA)
        }

        capaBloqueo.visibility = View.VISIBLE
    }

    private fun actualizarGrafica(registros: List<RegistroConDetalles>, filtro: FiltroTiempo) {
        val datos = lecheViewModel.agruparDatos(registros, filtro)
        
        val composeView = ComposeView(requireContext()).apply {
            setContent {
                BarChart(datos)
            }
        }
        
        binding.chartContainer.removeAllViews()
        binding.chartContainer.addView(composeView)
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
            
            // Espacio para etiquetas inferiores y superiores
            val bottomPadding = 40.dp.toPx()
            val topPadding = 30.dp.toPx()
            val availableHeight = canvasHeight - bottomPadding - topPadding
            
            val barCount = items.size.coerceAtLeast(1)
            val totalBarWidth = canvasWidth * 0.8f // Las barras ocupan el 80% del ancho
            val singleBarWidth = (totalBarWidth / barCount) * 0.6f
            val spacing = (totalBarWidth / barCount) * 0.4f
            val startOffset = (canvasWidth - totalBarWidth) / 2

            // Dibujar líneas de fondo (guía)
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

                // Dibujar la barra con bordes redondeados arriba
                drawRoundRect(
                    color = Color(0xFF4CAF50), // Verde más claro como en la imagen
                    topLeft = Offset(x, y),
                    size = Size(singleBarWidth, barHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )

                // Dibujar valor arriba de la barra
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

                // Dibujar etiqueta abajo de la barra
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
