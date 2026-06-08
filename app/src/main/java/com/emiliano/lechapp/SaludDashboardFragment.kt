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
import androidx.recyclerview.widget.LinearLayoutManager
import com.emiliano.lechapp.databinding.FragmentSaludDashboardBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SaludDashboardFragment : Fragment() {

    private var _binding: FragmentSaludDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LecheViewModel by activityViewModels {
        val database = AppDatabase.getDatabase(requireContext())
        LecheViewModel.Factory(
            database.usuarioDao(),
            database.registrosRelacionalesDao(),
            GeminiService(),
        )
    }

    private val rankingAdapter = RankingAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSaludDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        observarDatos()
    }

    private fun setupUI() {
        binding.rvTopRanking.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = rankingAdapter
        }
    }

    private fun observarDatos() {
        // Predicción Global (Ahora Total Litros de Hoy)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.prediccionGlobal.collectLatest { litros ->
                    binding.tvAmountPrediccionGlobal.text = "${litros.formatearMiles()} L"
                }
            }
        }

        // Alertas de Hato
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.alertaGlobal.collectLatest { alerta ->
                    if (alerta != null) {
                        binding.cardAlertaHato.visibility = View.VISIBLE
                        binding.tvAlertaHatoTexto.text = alerta
                    } else {
                        binding.cardAlertaHato.visibility = View.GONE
                    }
                }
            }
        }

        // Ranking Top 3
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.relacionalesDao.obtenerAnimalesConProduccionTotal().collectLatest { animales ->
                    val top3 = animales
                        .filter { it.animal != null && it.totalLitros > 0 }
                        .sortedByDescending { it.totalLitros }
                        .take(3)
                    rankingAdapter.submitList(top3)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
