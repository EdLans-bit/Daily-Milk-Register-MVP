package com.emiliano.lechapp

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.emiliano.lechapp.databinding.FragmentPerfilBinding
import kotlinx.coroutines.flow.collectLatest

class PerfilFragment : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LecheViewModel by activityViewModels {
        val database = AppDatabase.getDatabase(requireContext())
        LecheViewModel.Factory(database.usuarioDao(), GeminiService())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("lechapp_prefs", Context.MODE_PRIVATE)
        val precioActual = prefs.getString("precio_maestro", "0.0")
        binding.etPrecioMaestro.setText(precioActual)

        binding.btnGuardarPrecio.setOnClickListener {
            val nuevoPrecio = binding.etPrecioMaestro.text.toString()
            if (nuevoPrecio.isNotBlank()) {
                prefs.edit().putString("precio_maestro", nuevoPrecio).apply()
                Toast.makeText(requireContext(), "Precio Maestro guardado: $nuevoPrecio", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnBorrarHistorial.setOnClickListener {
            mostrarDialogoBorrado()
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.perfilUsuario.collectLatest { perfil ->
                perfil?.let {
                    binding.tvNombreUsuario.text = it.nombreGanadero
                    binding.tvAnimales.text = "${it.cantidadAnimales} Animales"
                }
            }
        }
    }

    private fun mostrarDialogoBorrado() {
        AlertDialog.Builder(requireContext())
            .setTitle("¡CUIDADO!")
            .setMessage("¿Estás seguro de que deseas borrar TODO el historial? Esta acción no se puede deshacer.")
            .setPositiveButton("BORRAR TODO") { _, _ ->
                viewModel.borrarTodoElHistorial()
                Toast.makeText(requireContext(), "Historial borrado", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("CANCELAR", null)
            .show()
            .apply {
                getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(resources.getColor(R.color.btn_danger_text, null))
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
