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

    // Mantenemos tu conexión exacta a Room y al servicio predictivo
    private val viewModel: LecheViewModel by activityViewModels {
        val database = AppDatabase.getDatabase(requireContext())
        LecheViewModel.Factory(
            database.usuarioDao(),
            database.registrosRelacionalesDao(),
            GeminiService() // O el servicio que estés usando
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

        configurarRanking()
        configurarBloqueoPremium()
        observarMotorInteligente()
    }

    private fun configurarRanking() {
        binding.rvRankingVacas.layoutManager = LinearLayoutManager(requireContext())

        // Cargamos la lista de animales desde Room
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.relacionalesDao.getAllAnimalesLotes().collectLatest { animales ->
                    listaAnimales = animales

                    // TODO: Aquí debes asignar tu adaptador (ej. RankingAdapter)
                    // binding.rvRankingVacas.adapter = TuAdaptador(listaAnimales) { vacaSeleccionada ->
                    //     binding.tvNombreVacaSeleccionada.text = "Vaca Seleccionada: ${vacaSeleccionada.identificador}"
                    //     viewModel.generarAnalisis(vacaSeleccionada.idAnimal)
                    // }
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