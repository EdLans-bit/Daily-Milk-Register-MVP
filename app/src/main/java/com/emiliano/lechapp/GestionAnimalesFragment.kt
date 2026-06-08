package com.emiliano.lechapp

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.emiliano.lechapp.databinding.DialogAddAnimalBinding
import com.emiliano.lechapp.databinding.FragmentGestionAnimalesBinding
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

class GestionAnimalesFragment : Fragment() {

    private var _binding: FragmentGestionAnimalesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LecheViewModel by activityViewModels {
        val database = AppDatabase.getDatabase(requireContext())
        LecheViewModel.Factory(
            database.usuarioDao(),
            database.registrosRelacionalesDao(),
            GeminiService()
        )
    }

    private val selectionAdapter = VacaSelectionAdapter { item ->
        seleccionarAnimal(item)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGestionAnimalesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvSeleccionVaca.adapter = selectionAdapter

        binding.btnBloquearPrediccion.setOnClickListener {
            binding.tvMensajePremium.visibility = View.VISIBLE
            Toast.makeText(requireContext(), "Activa Premium para desbloquear", Toast.LENGTH_SHORT).show()
        }

        setupChart()
        observarDatos()
    }

    private fun setupChart() {
        binding.chartTendenciaVaca.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            xAxis.isEnabled = false
            axisRight.isEnabled = false
            setDrawGridBackground(false)
        }
    }

    private fun seleccionarAnimal(item: AnimalConProduccion) {
        viewModel.generarAnalisis(item.animal.idAnimal)
        viewModel.seleccionarAnimalParaHistorial(item.animal.idAnimal)
        binding.tvNombreAnimalSeleccionado.text = "Vaca ${item.animal.identificador}"
        mostrarDetalleAnimal(item)
    }

    private fun mostrarDetalleAnimal(item: AnimalConProduccion) {
        val sb = StringBuilder()
        sb.append("**Producción hoy:** ${String.format(Locale.getDefault(), "%.1f", item.litrosHoy)} Litros\n")
        sb.append("**Promedio:** ${String.format(Locale.getDefault(), "%.1f", item.promedioLitros)} L/día\n")
        sb.append("**Total histórico:** ${String.format(Locale.getDefault(), "%.1f", item.totalLitros)} L")
        
        binding.tvResumenProduccionIndividual.text = TextoUtils.formatearMarkdown(sb.toString())
    }

    private fun observarDatos() {
        // Lista de Animales y Selección
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.relacionalesDao.obtenerAnimalesConProduccionTotal().collectLatest { animales ->
                    // Actualizar Selección (arriba)
                    selectionAdapter.submitList(animales)
                    if (animales.isNotEmpty() && viewModel.historialAnimalSeleccionado.value.isEmpty()) {
                        selectionAdapter.selectFirst()
                    }
                }
            }
        }

        // Historial Individual (Gráfica)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.historialAnimalSeleccionado.collectLatest { historial ->
                    actualizarGrafica(historial)
                }
            }
        }
        
        // Predicciones (Basadas en el estado del ViewModel y Premium)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.relacionalesDao.obtenerAnimalesConProduccionTotal().collectLatest { animales ->
                    actualizarPredicciones(animales)
                }
            }
        }

        // Estado Premium
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.esUsuarioPremium.collectLatest { isPremium ->
                    if (isPremium) {
                        binding.btnBloquearPrediccion.visibility = View.GONE
                        binding.tvMensajePremium.visibility = View.GONE
                        binding.tvPrediccionVacas.alpha = 1.0f
                    } else {
                        binding.btnBloquearPrediccion.visibility = View.VISIBLE
                        binding.tvPrediccionVacas.alpha = 0.3f
                    }
                }
            }
        }
    }

    private fun actualizarPredicciones(animales: List<AnimalConProduccion>) {
        val sb = StringBuilder()
        animales.take(3).forEach { animal ->
            // Simulación de predicción simple para la UI
            val esBaja = animal.animal.idAnimal % 2 == 0 // Solo para variar en el ejemplo
            val porcentaje = if (esBaja) "-5%" else "+2%"
            val estado = if (esBaja) "Riesgo de baja" else "Estable"
            
            sb.append("• Vaca ${animal.animal.identificador}: $estado ($porcentaje)\n")
        }
        if (animales.isEmpty()) sb.append("No hay suficientes datos para predicciones.")
        binding.tvPrediccionVacas.text = sb.toString().trim()
    }

    private fun actualizarGrafica(historial: List<Double>) {
        if (historial.isEmpty()) {
            binding.chartTendenciaVaca.visibility = View.GONE
            binding.tvChartPlaceholder.visibility = View.VISIBLE
            return
        }
        
        binding.chartTendenciaVaca.visibility = View.VISIBLE
        binding.tvChartPlaceholder.visibility = View.GONE
        
        val entries = historial.mapIndexed { index, litros ->
            Entry(index.toFloat(), litros.toFloat())
        }

        val dataSet = LineDataSet(entries, "Producción").apply {
            color = resources.getColor(R.color.primary_green, null)
            setCircleColor(resources.getColor(R.color.primary_green, null))
            lineWidth = 3f
            setDrawFilled(true)
            fillColor = resources.getColor(R.color.bg_badge, null)
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        binding.chartTendenciaVaca.data = LineData(dataSet)
        binding.chartTendenciaVaca.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
