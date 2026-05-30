package com.emiliano.lechapp

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.emiliano.lechapp.databinding.FragmentRegistroBinding
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*

class RegistroFragment : Fragment() {

    private var _binding: FragmentRegistroBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LecheViewModel by activityViewModels {
        val database = AppDatabase.getDatabase(requireContext())
        LecheViewModel.Factory(
            database.usuarioDao(),
            database.registrosRelacionalesDao(),
            GeminiService()
        )
    }
    private var fechaSeleccionada = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegistroBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        actualizarFechaEnUI()

        binding.btnFecha.setOnClickListener {
            mostrarDatePicker()
        }

        binding.btnMicGiant.setOnClickListener {
            viewModel.activarMicrofonoDesdeHardware()
        }

        binding.btnGuardar.setOnClickListener {
            guardarRegistroManual()
        }

        // Cargar precio maestro desde SharedPreferences
        val prefs = requireContext().getSharedPreferences("lechapp_prefs", Context.MODE_PRIVATE)
        val precioMaestro = prefs.getString("precio_maestro", "0.0") ?: "0.0"
        
        // Observar trigger de micrófono (hardware o botón gigante)
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.triggerMicrofono.collectLatest {
                (activity as? MainActivity)?.iniciarReconocimientoVoz()
            }
        }
    }

    private fun mostrarDatePicker() {
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                fechaSeleccionada.set(year, month, dayOfMonth)
                actualizarFechaEnUI()
            },
            fechaSeleccionada.get(Calendar.YEAR),
            fechaSeleccionada.get(Calendar.MONTH),
            fechaSeleccionada.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun actualizarFechaEnUI() {
        val sdf = SimpleDateFormat("dd / MM / yyyy", Locale.getDefault())
        binding.btnFecha.text = sdf.format(fechaSeleccionada.time)
    }

    fun guardarRegistroManual() {
        val cantidad = binding.etCantidad.text.toString()
        val comprador = binding.etComprador.text.toString()
        val fecha = binding.btnFecha.text.toString()
        
        val prefs = requireContext().getSharedPreferences("lechapp_prefs", Context.MODE_PRIVATE)
        val precio = prefs.getString("precio_maestro", "0.0") ?: "0.0"

        viewModel.guardarRegistroManual(requireContext(), cantidad, precio, comprador, fecha)
        
        // Limpiar campos
        binding.etCantidad.text?.clear()
        binding.etComprador.text?.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}