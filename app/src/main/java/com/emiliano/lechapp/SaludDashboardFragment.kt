package com.emiliano.lechapp

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.emiliano.lechapp.databinding.FragmentSaludDashboardBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SaludDashboardFragment : Fragment() {

    private var _binding: FragmentSaludDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LecheViewModel by activityViewModels {
        val database = AppDatabase.getDatabase(requireContext())
        LecheViewModel.Factory(
            database.usuarioDao(),
            database.registrosRelacionalesDao(),
            GeminiService()
        )
    }

    private var listaAnimales: List<AnimalLote> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSaludDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDropdown()
    }

    private fun setupDropdown() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.relacionalesDao.getAllAnimalesLotes().collectLatest { animales ->
                    listaAnimales = animales
                    val nombres = animales.map { "${it.identificador} - ${it.raza ?: "Sin Raza"}" }
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, nombres)
                    binding.autoCompleteAnimal.setAdapter(adapter)
                }
            }
        }

        binding.autoCompleteAnimal.setOnItemClickListener { _, _, position, _ ->
            if (position < listaAnimales.size) {
                val animalSeleccionado = listaAnimales[position]
                actualizarEstadoSalud(animalSeleccionado.idAnimal)
            }
        }
    }

    private fun actualizarEstadoSalud(animalId: Int) {
        viewModel.analizarSaludAnimal(animalId) { estado, registros ->
            // Verificación de seguridad para evitar NPE si el fragmento se destruyó
            _binding?.let { b ->
                b.tvEstadoSalud.text = estado
                actualizarGrafico(registros)
                
                when {
                    estado.contains("Alerta", ignoreCase = true) -> {
                        b.layoutEstadoSalud.setBackgroundColor(Color.parseColor("#FFEBEE"))
                        b.imgSaludIcon.setImageResource(android.R.drawable.stat_sys_warning)
                        b.imgSaludIcon.setColorFilter(Color.parseColor("#F44336"))
                        b.tvEstadoSalud.setTextColor(Color.parseColor("#D32F2F"))
                    }
                    estado == "Saludable" -> {
                        b.layoutEstadoSalud.setBackgroundColor(Color.parseColor("#E8F5E9"))
                        b.imgSaludIcon.setImageResource(android.R.drawable.checkbox_on_background)
                        b.imgSaludIcon.setColorFilter(Color.parseColor("#4CAF50"))
                        b.tvEstadoSalud.setTextColor(Color.parseColor("#2E7D32"))
                    }
                    else -> {
                        b.layoutEstadoSalud.setBackgroundColor(Color.parseColor("#F5F5F5"))
                        b.imgSaludIcon.setImageResource(android.R.drawable.ic_dialog_info)
                        b.imgSaludIcon.setColorFilter(Color.parseColor("#757575"))
                        b.tvEstadoSalud.setTextColor(Color.parseColor("#333333"))
                    }
                }
            }
        }
    }

    private fun actualizarGrafico(registros: List<RegistroLeche>) {
        val chart = _binding?.lineChartProduccion ?: return
        
        if (registros.isEmpty()) {
            chart.clear()
            chart.setNoDataText("No hay datos para graficar")
            return
        }

        val registrosOrdenados = registros.sortedBy { it.fecha }
        val entries = registrosOrdenados.mapIndexed { index, registro ->
            Entry(index.toFloat(), registro.litros.toFloat())
        }

        val dataSet = LineDataSet(entries, "Producción").apply {
            color = Color.parseColor("#4CAF50")
            setCircleColor(Color.parseColor("#4CAF50"))
            lineWidth = 3f
            circleRadius = 5f
            setDrawCircleHole(false)
            valueTextSize = 10f
            setDrawFilled(true)
            fillColor = Color.parseColor("#4CAF50")
            fillAlpha = 30
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawValues(true)
        }

        chart.data = LineData(dataSet)
        
        chart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            axisRight.isEnabled = false
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                setDrawAxisLine(true)
                valueFormatter = object : ValueFormatter() {
                    val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
                    override fun getFormattedValue(value: Float): String {
                        val i = value.toInt()
                        return if (i >= 0 && i < registrosOrdenados.size) {
                            sdf.format(Date(registrosOrdenados[i].fecha))
                        } else ""
                    }
                }
            }
            
            axisLeft.apply {
                setDrawGridLines(false)
                axisMinimum = 0f
                setDrawAxisLine(true)
            }
            
            setTouchEnabled(true)
            setPinchZoom(false)
            setScaleEnabled(false)
            animateX(800)
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
