package com.emiliano.lechapp

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
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
        val precioActual = prefs.getString("precio_maestro", "1200")
        binding.etPrecioMaestro.setText(precioActual)
        
        val sensibilidadActual = prefs.getString("sensibilidad_alertas", "15")
        binding.etSensibilidadAlertas.setText(sensibilidadActual)

        binding.btnGuardarConfiguracion.setOnClickListener {
            val nuevoPrecio = binding.etPrecioMaestro.text.toString()
            val nuevaSensibilidad = binding.etSensibilidadAlertas.text.toString()
            
            if (nuevoPrecio.isNotBlank() && nuevaSensibilidad.isNotBlank()) {
                prefs.edit().apply {
                    putString("precio_maestro", nuevoPrecio)
                    putString("sensibilidad_alertas", nuevaSensibilidad)
                    apply()
                }
                viewModel.actualizarPrecioPorDefecto(nuevoPrecio.toDoubleOrNull() ?: 2000.0)
                viewModel.sensibilidadAlertas.value = nuevaSensibilidad.toDoubleOrNull() ?: 15.0
                
                Toast.makeText(requireContext(), "Configuración guardada", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnBorrarHistorial.setOnClickListener {
            mostrarDialogoBorrado()
        }

        binding.btnGestionarVacas.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, GestionAnimalesFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnGestionarCompradores.setOnClickListener {
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
                getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(requireContext(), R.color.alert_border_text))
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
