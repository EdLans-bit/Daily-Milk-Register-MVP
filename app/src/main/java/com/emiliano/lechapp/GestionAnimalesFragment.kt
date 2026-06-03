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
import com.emiliano.lechapp.databinding.FragmentGestionAnimalesBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class GestionAnimalesFragment : Fragment() {

    private var _binding: FragmentGestionAnimalesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LecheViewModel by activityViewModels {
        val database = AppDatabase.getDatabase(requireContext())
        LecheViewModel.Factory(
            database.usuarioDao(),
            database.registrosRelacionalesDao(),
            GeminiService()
        )
    }

    private val adapter = AnimalAdapter(
        onItemClick = { item -> mostrarFichaSalud(item) },
        onDeleteClick = { animal -> mostrarConfirmacionBorrado(animal) }
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGestionAnimalesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvAnimales.adapter = adapter

        binding.fabAddAnimal.setOnClickListener {
            mostrarDialogoNuevoAnimal()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.relacionalesDao.obtenerAnimalesConProduccionTotal().collectLatest { animales ->
                    adapter.submitList(animales)
                }
            }
        }
    }

    private fun mostrarFichaSalud(item: AnimalConProduccion) {
        val dialogBinding = com.emiliano.lechapp.databinding.DialogFichaSaludBinding.inflate(layoutInflater)
        val animal = item.animal
        
        dialogBinding.tvFichaNombre.text = "Nombre: ${animal.identificador}"
        dialogBinding.tvFichaRaza.text = "Raza: ${animal.raza ?: "Sin definir"}"
        
        // Mostrar promedio y total de la ficha
        val infoCorta = String.format(java.util.Locale.getDefault(), 
            "Total: %.1f L | Promedio: %.1f L (%d ordeños)", 
            item.totalLitros, item.promedioLitros, item.conteoRegistros)
        
        // Simulación de estado basada en datos reales
        viewModel.analizarSaludAnimal(animal.idAnimal) { resultado, registros ->
            dialogBinding.tvFichaEstado.text = "Estado: $resultado"
            if (resultado.contains("Alerta")) {
                dialogBinding.tvFichaEstado.setTextColor(resources.getColor(R.color.alert_border_text, null))
            } else {
                dialogBinding.tvFichaEstado.setTextColor(resources.getColor(R.color.primary_green, null))
            }
            
            dialogBinding.tvFichaHistorial.text = "$infoCorta\n\nResumen semanal: " + (if (registros.isNotEmpty()) {
                "Últimos 7 días con ${registros.size} registros."
            } else {
                "Sin registros recientes."
            })
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()
            
        dialogBinding.btnCerrarFicha.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun mostrarConfirmacionBorrado(animal: AnimalLote) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Animal")
            .setMessage("¿Estás seguro de que deseas eliminar a ${animal.identificador}? Se perderán sus vínculos con los registros de leche.")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.borrarAnimal(animal)
                Toast.makeText(requireContext(), "Animal eliminado", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoNuevoAnimal() {
        val dialogBinding = DialogAddAnimalBinding.inflate(layoutInflater)
        
        // Configurar Spinner con las razas
        val razasAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            AnimalLote.RAZAS_COMUNES
        )
        dialogBinding.spinnerRaza.adapter = razasAdapter

        AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = dialogBinding.etIdentificador.text.toString()
                val esLote = dialogBinding.swEsLote.isChecked
                val raza = dialogBinding.spinnerRaza.selectedItem.toString()

                if (nombre.isNotBlank()) {
                    guardarAnimal(nombre, esLote, raza)
                } else {
                    Toast.makeText(requireContext(), "El nombre es obligatorio", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun guardarAnimal(nombre: String, esLote: Boolean, raza: String) {
        lifecycleScope.launch {
            val nuevoAnimal = AnimalLote(
                identificador = nombre,
                esLoteGeneral = esLote,
                raza = raza
            )
            viewModel.relacionalesDao.insertAnimalLote(nuevoAnimal)
            Toast.makeText(requireContext(), "Animal guardado correctamente", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
