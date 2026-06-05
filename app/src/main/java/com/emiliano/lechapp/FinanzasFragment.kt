package com.emiliano.lechapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.emiliano.lechapp.databinding.FragmentFinanzasBinding
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

class FinanzasFragment : Fragment() {

    private var _binding: FragmentFinanzasBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LecheViewModel by activityViewModels {
        val database = AppDatabase.getDatabase(requireContext())
        LecheViewModel.Factory(
            database.usuarioDao(),
            database.registrosRelacionalesDao(),
            GeminiService()
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFinanzasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupChart()
        observarDatos()
        viewModel.calcularRentabilidadSemanal()
    }

    private fun setupChart() {
        binding.chartFinanzas.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            legend.isEnabled = true
            axisRight.isEnabled = false
            xAxis.apply {
                granularity = 1f
                setDrawGridLines(false)
                position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
            }
        }
    }

    private fun observarDatos() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.balanceSemanal.collectLatest { balance ->
                        binding.tvBalanceTotal.text = String.format(Locale.getDefault(), "$ %.0f", balance)
                    }
                }
                launch {
                    viewModel.ingresosSemanales.collectLatest { valo ->
                        binding.tvIngresos.text = String.format(Locale.getDefault(), "$ %.0f", valo)
                    }
                }
                launch {
                    viewModel.gastosSemanales.collectLatest { valo ->
                        binding.tvGastos.text = String.format(Locale.getDefault(), "$ %.0f", valo)
                    }
                }
                launch {
                    viewModel.balanceFinanciero.collectLatest { datos ->
                        actualizarGrafica(datos)
                    }
                }
            }
        }
    }

    private fun actualizarGrafica(datos: List<BalanceDiario>) {
        if (datos.isEmpty()) return

        val entriesIngresos = mutableListOf<BarEntry>()
        val entriesGastos = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()

        datos.forEachIndexed { index, balance ->
            entriesIngresos.add(BarEntry(index.toFloat(), balance.ingresos.toFloat()))
            entriesGastos.add(BarEntry(index.toFloat(), balance.gastos.toFloat()))
            labels.add(balance.dia.takeLast(5)) // Solo MM-DD
        }

        val setIngresos = BarDataSet(entriesIngresos, "Ingresos").apply {
            color = resources.getColor(R.color.primary_green, null)
        }
        val setGastos = BarDataSet(entriesGastos, "Gastos").apply {
            color = resources.getColor(R.color.alert_red, null)
        }

        val barData = BarData(setIngresos, setGastos)
        val groupSpace = 0.3f
        val barSpace = 0.05f
        val barWidth = 0.3f
        
        barData.barWidth = barWidth
        
        binding.chartFinanzas.apply {
            data = barData
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            groupBars(0f, groupSpace, barSpace)
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
