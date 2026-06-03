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
            GeminiService()
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSaludDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configurarRanking()
        configurarBloqueoPremium()
        observarMotorInteligente()
        configurarAlertasHato()
    }

    private fun configurarAlertasHato() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.registrosFiltrados.collectLatest { registros ->
                    if (registros.isEmpty()) {
                        binding.cardAlertaHato.visibility = View.GONE
                        binding.tvSinAlertas.visibility = View.VISIBLE
                        return@collectLatest
                    }

                    // Calculamos una alerta de hato simple: comparamos hoy con ayer
                    val hoy = System.currentTimeMillis()
                    val ayer = hoy - (24 * 60 * 60 * 1000L)
                    
                    val prodHoy = registros.filter { isSameDay(it.registro.fecha, hoy) }.sumOf { it.registro.litros }
                    val prodAyer = registros.filter { isSameDay(it.registro.fecha, ayer) }.sumOf { it.registro.litros }

                    if (prodAyer > 0 && prodHoy < prodAyer * 0.85) {
                        val caida = ((prodAyer - prodHoy) / prodAyer) * 100
                        binding.tvAlertaHatoTexto.text = String.format(java.util.Locale.getDefault(), "La producción total bajó un %.1f%% respecto a ayer.", caida)
                        binding.cardAlertaHato.visibility = View.VISIBLE
                        binding.tvSinAlertas.visibility = View.GONE
                    } else {
                        binding.cardAlertaHato.visibility = View.GONE
                        binding.tvSinAlertas.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun isSameDay(t1: Long, t2: Long): Boolean {
        val c1 = java.util.Calendar.getInstance().apply { timeInMillis = t1 }
        val c2 = java.util.Calendar.getInstance().apply { timeInMillis = t2 }
        return c1.get(java.util.Calendar.YEAR) == c2.get(java.util.Calendar.YEAR) &&
               c1.get(java.util.Calendar.DAY_OF_YEAR) == c2.get(java.util.Calendar.DAY_OF_YEAR)
    }

    private fun configurarRanking() {
        binding.rvRankingVacas.layoutManager = LinearLayoutManager(requireContext())
        val adapter = RankingAdapter { vacaSeleccionada ->
            binding.tvNombreVacaSeleccionada.text = "Vaca Seleccionada: ${vacaSeleccionada.identificador}"
            viewModel.generarAnalisis(vacaSeleccionada.idAnimal)
        }
        binding.rvRankingVacas.adapter = adapter

        // Cargamos la lista de animales con su producción total desde Room
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.relacionalesDao.obtenerAnimalesConProduccionTotal().collectLatest { animalesConProduccion ->
                    adapter.submitList(animalesConProduccion)
                }
            }
        }
    }

    private fun configurarBloqueoPremium() {
        // Monitoreamos si el usuario es Premium para quitar el candado
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.esUsuarioPremium.collectLatest { esPremium ->
                    if (esPremium) {
                        binding.capaBloqueoSalud.visibility = View.GONE
                        binding.layoutAnalisisIndividual.alpha = 1.0f
                    } else {
                        binding.capaBloqueoSalud.visibility = View.VISIBLE
                        binding.layoutAnalisisIndividual.alpha = 0.3f
                    }
                }
            }
        }

        // Simulación para la sustentación del proyecto
        binding.btnDesbloquearSalud.setOnClickListener {
            viewModel.activarModoDemoPremium()
            android.widget.Toast.makeText(
                requireContext(),
                "¡Análisis Individual Desbloqueado!",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun observarMotorInteligente() {
        // Escuchamos los resultados del análisis matemático individual
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.estadoPredictivo.collectLatest { resultado ->
                    if (resultado == null) {
                        binding.tvPrediccionIndividual.text = "Selecciona un animal"
                        binding.tvInsightIndividual.text = "Esperando selección para procesar análisis..."
                        binding.tvAlertaIndividual.visibility = View.GONE
                        return@collectLatest
                    }

                    // Inyectamos los datos procesados en la vista lineal
                    binding.tvPrediccionIndividual.text = "Mañana: ${resultado.litrosPredichos}L"
                    binding.tvInsightIndividual.text = resultado.insightTexto

                    // Verificamos si la alerta supera el límite de sensibilidad de la configuración
                    val sensibilidadParametrizada = viewModel.sensibilidadAlertas.value

                    if (resultado.porcentajeCaida >= sensibilidadParametrizada) {
                        binding.tvAlertaIndividual.text = "Atención: Caída del ${resultado.porcentajeCaida}% en la producción de este animal."
                        binding.tvAlertaIndividual.visibility = View.VISIBLE
                    } else {
                        binding.tvAlertaIndividual.visibility = View.GONE
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}