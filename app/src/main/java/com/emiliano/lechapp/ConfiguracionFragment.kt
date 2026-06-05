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
import com.emiliano.lechapp.databinding.FragmentConfiguracionBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ConfiguracionFragment : Fragment() {

    private var _binding: FragmentConfiguracionBinding? = null
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
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentConfiguracionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cardGestionarAnimales.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, ListaAnimalesFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.cardGestionarCompradores.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, GestionCompradoresFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnGuardarConfig.setOnClickListener {
            val precio = binding.etPrecioMaestro.text.toString().toDoubleOrNull() ?: 0.0
            viewModel.actualizarPrecioPorDefecto(precio)
            Toast.makeText(requireContext(), "Configuración Guardada", Toast.LENGTH_SHORT).show()
        }

        binding.btnBorrarHistorial.setOnClickListener {
            mostrarConfirmacionBorradoTodo()
        }

        observarDatos()
    }

    private fun observarDatos() {
        // Conteo de animales
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.relacionalesDao.getAllAnimalesLotes().collectLatest { animales ->
                    binding.tvConteoAnimales.text = "Total: ${animales.size} animales registrados"
                }
            }
        }

        // Cargar precio actual
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.precioPorDefecto.collectLatest { precio ->
                    if (binding.etPrecioMaestro.text.toString().isEmpty() || binding.etPrecioMaestro.text.toString() == "5000") {
                        binding.etPrecioMaestro.setText(precio.toInt().toString())
                    }
                }
            }
        }
    }

    private fun mostrarConfirmacionBorradoTodo() {
        AlertDialog.Builder(requireContext())
            .setTitle("Borrar Todo")
            .setMessage("¿Estás seguro de borrar todo el historial? Esta acción no se puede deshacer.")
            .setPositiveButton("Borrar") { _, _ ->
                viewModel.borrarTodoElHistorial()
                Toast.makeText(requireContext(), "Historial Borrado", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
