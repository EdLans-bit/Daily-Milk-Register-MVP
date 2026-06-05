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
import androidx.recyclerview.widget.LinearLayoutManager
import com.emiliano.lechapp.databinding.DialogAddAnimalBinding
import com.emiliano.lechapp.databinding.FragmentListaAnimalesBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

import com.emiliano.lechapp.databinding.DialogDetalleAnimalBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ListaAnimalesFragment : Fragment() {

    private var _binding: FragmentListaAnimalesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LecheViewModel by activityViewModels {
        val database = AppDatabase.getDatabase(requireContext())
        LecheViewModel.Factory(
            database.usuarioDao(),
            database.registrosRelacionalesDao(),
            GeminiService()
        )
    }

    private val adapter = AnimalGestionAdapter(
        onItemClick = { item -> mostrarDetalleAnimal(item) },
        onDeleteClick = { animal -> mostrarConfirmacionBorrado(animal) }
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListaAnimalesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvAnimales.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAnimales.adapter = adapter

        binding.fabAddAnimal.setOnClickListener {
            mostrarDialogoNuevoAnimal()
        }

        observarDatos()
    }

    private fun mostrarDetalleAnimal(item: AnimalConProduccion) {
        val dialogBinding = DialogDetalleAnimalBinding.inflate(layoutInflater)
        val animal = item.animal

        lifecycleScope.launch {
            // Obtener registros para calcular max/min
            val registros = viewModel.relacionalesDao.obtenerRegistrosRecientesAnimal(animal.idAnimal, 0L)
            val litros = registros.map { it.litros }
            
            val max = if (litros.isNotEmpty()) litros.maxOrNull() else 0.0
            val min = if (litros.isNotEmpty()) litros.minOrNull() else 0.0
            
            dialogBinding.tvTituloDetalle.text = "Detalle: ${animal.identificador}"
            dialogBinding.tvLitrosTotales.text = "Litros totales: ${String.format("%.1f", item.totalLitros)} L"
            dialogBinding.tvProduccionMaxima.text = "Máximo en un día: ${String.format("%.1f", max)} L"
            dialogBinding.tvProduccionMinima.text = "Mínimo en un día: ${String.format("%.1f", min)} L"
            
            val fechaFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(animal.fechaRegistro))
            dialogBinding.tvFechaRegistro.text = "Registrado el: $fechaFmt"

            AlertDialog.Builder(requireContext())
                .setView(dialogBinding.root)
                .setPositiveButton("Cerrar", null)
                .show()
        }
    }

    private fun observarDatos() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.relacionalesDao.obtenerAnimalesConProduccionTotal().collectLatest { lista ->
                    adapter.submitList(lista)
                }
            }
        }
    }

    private fun mostrarDialogoNuevoAnimal() {
        val dialogBinding = DialogAddAnimalBinding.inflate(layoutInflater)
        val razasAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, AnimalLote.RAZAS_COMUNES)
        dialogBinding.spinnerRaza.adapter = razasAdapter

        AlertDialog.Builder(requireContext())
            .setTitle("Nuevo Animal")
            .setView(dialogBinding.root)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = dialogBinding.etIdentificador.text.toString()
                val esLote = dialogBinding.swEsLote.isChecked
                val raza = dialogBinding.spinnerRaza.selectedItem.toString()
                if (nombre.isNotBlank()) {
                    guardarAnimal(nombre, esLote, raza)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun guardarAnimal(nombre: String, esLote: Boolean, raza: String) {
        lifecycleScope.launch {
            val nuevoAnimal = AnimalLote(identificador = nombre, esLoteGeneral = esLote, raza = raza)
            viewModel.relacionalesDao.insertAnimalLote(nuevoAnimal)
            Toast.makeText(requireContext(), "Animal guardado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarConfirmacionBorrado(animal: AnimalLote) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Animal")
            .setMessage("¿Estás seguro de eliminar a ${animal.identificador}?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.borrarAnimal(animal)
                Toast.makeText(requireContext(), "Animal eliminado", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
