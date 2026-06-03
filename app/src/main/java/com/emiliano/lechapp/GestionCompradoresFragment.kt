package com.emiliano.lechapp

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.emiliano.lechapp.databinding.DialogAddCompradorBinding
import com.emiliano.lechapp.databinding.FragmentGestionCompradoresBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class GestionCompradoresFragment : Fragment() {

    private var _binding: FragmentGestionCompradoresBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LecheViewModel by activityViewModels {
        val database = AppDatabase.getDatabase(requireContext())
        LecheViewModel.Factory(
            database.usuarioDao(),
            database.registrosRelacionalesDao(),
            GeminiService()
        )
    }

    private val adapter = CompradorAdapter(
        onEditClick = { comprador -> mostrarDialogoNuevoComprador(comprador) },
        onDeleteClick = { comprador -> mostrarConfirmacionBorrado(comprador) }
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGestionCompradoresBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvCompradores.adapter = adapter

        binding.fabAddComprador.setOnClickListener {
            mostrarDialogoNuevoComprador()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.relacionalesDao.getAllCompradores().collectLatest { compradores ->
                    adapter.submitList(compradores)
                }
            }
        }
    }

    private fun mostrarConfirmacionBorrado(comprador: Comprador) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Comprador")
            .setMessage("¿Estás seguro de que deseas eliminar a ${comprador.nombre}?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.borrarComprador(comprador)
                Toast.makeText(requireContext(), "Comprador eliminado", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoNuevoComprador(compradorAEditar: Comprador? = null) {
        val dialogBinding = DialogAddCompradorBinding.inflate(layoutInflater)
        
        // Si estamos editando, pre-llenamos los campos
        compradorAEditar?.let {
            dialogBinding.etNombre.setText(it.nombre)
            dialogBinding.etPrecioBase.setText(it.precioBase.toString())
            dialogBinding.etTelefono.setText(it.telefono ?: "")
        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (compradorAEditar == null) "Nuevo Comprador" else "Editar Comprador")
            .setView(dialogBinding.root)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = dialogBinding.etNombre.text.toString()
                val precio = dialogBinding.etPrecioBase.text.toString().toDoubleOrNull() ?: 0.0
                val telefono = dialogBinding.etTelefono.text.toString().takeIf { it.isNotBlank() }

                if (nombre.isNotBlank()) {
                    if (compradorAEditar == null) {
                        guardarComprador(nombre, precio, telefono)
                    } else {
                        actualizarComprador(compradorAEditar.copy(
                            nombre = nombre,
                            precioBase = precio,
                            telefono = telefono
                        ))
                    }
                } else {
                    Toast.makeText(requireContext(), "El nombre es obligatorio", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun guardarComprador(nombre: String, precio: Double, telefono: String?) {
        lifecycleScope.launch {
            val nuevoComprador = Comprador(
                nombre = nombre,
                precioBase = precio,
                telefono = telefono
            )
            viewModel.relacionalesDao.insertComprador(nuevoComprador)
            Toast.makeText(requireContext(), "Comprador guardado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun actualizarComprador(comprador: Comprador) {
        lifecycleScope.launch {
            viewModel.relacionalesDao.updateComprador(comprador)
            Toast.makeText(requireContext(), "Comprador actualizado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
