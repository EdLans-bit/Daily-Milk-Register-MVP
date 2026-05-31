package com.emiliano.lechapp

import android.app.DatePickerDialog
import android.content.Context
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
import com.emiliano.lechapp.databinding.FragmentRegistroBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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
        setupDropdowns()

        binding.btnFecha.setOnClickListener {
            mostrarDatePicker()
        }

        binding.btnMicGiant.setOnClickListener {
            viewModel.activarMicrofonoDesdeHardware()
        }

        binding.btnGuardar.setOnClickListener {
            guardarRegistroManual()
        }

        // Observar trigger de micrófono (hardware o botón gigante)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.triggerMicrofono.collectLatest {
                    (activity as? MainActivity)?.iniciarReconocimientoVoz()
                }
            }
        }
    }

    private fun setupDropdowns() {
        // Observar Animales
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.relacionalesDao.getAllAnimalesLotes().collectLatest { lista ->
                    val nombres = lista.map { it.identificador }
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, nombres)
                    binding.etAnimal.setAdapter(adapter)
                }
            }
        }

        // Observar Compradores
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.relacionalesDao.getAllCompradores().collectLatest { lista ->
                    val nombres = lista.map { it.nombre }
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, nombres)
                    binding.etComprador.setAdapter(adapter)
                }
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
        val animal = binding.etAnimal.text.toString()
        val comprador = binding.etComprador.text.toString()
        val fecha = binding.btnFecha.text.toString()
        
        val prefs = requireContext().getSharedPreferences("lechapp_prefs", Context.MODE_PRIVATE)
        val precio = prefs.getString("precio_maestro", "0.0") ?: "0.0"

        viewModel.guardarRegistroManual(requireContext(), cantidad, precio, animal, comprador, fecha)
        
        // Limpiar campos
        binding.etCantidad.text?.clear()
        binding.etAnimal.text?.clear()
        binding.etComprador.text?.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
