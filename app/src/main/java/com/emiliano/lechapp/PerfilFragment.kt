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
import com.emiliano.lechapp.databinding.FragmentPerfilBinding

class PerfilFragment : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
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

        binding.cardGestionarAnimales.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, GestionAnimalesFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.cardGestionarCompradores.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, GestionCompradoresFragment())
                .addToBackStack(null)
                .commit()
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
