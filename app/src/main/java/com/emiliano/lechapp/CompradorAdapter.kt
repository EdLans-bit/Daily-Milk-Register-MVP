package com.emiliano.lechapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.emiliano.lechapp.databinding.ItemCompradorBinding
import java.util.Locale

class CompradorAdapter(private val onDeleteClick: (Comprador) -> Unit) : ListAdapter<Comprador, CompradorAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCompradorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onDeleteClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemCompradorBinding,
        private val onDeleteClick: (Comprador) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(comprador: Comprador) {
            binding.tvNombreComprador.text = comprador.nombre
            val precioFormateado = String.format(Locale.getDefault(), "$ %.2f", comprador.precioBase)
            val telefono = comprador.telefono ?: "Sin teléfono"
            binding.tvPrecioYTelefono.text = "$precioFormateado | $telefono"

            binding.btnBorrarComprador.setOnClickListener {
                onDeleteClick(comprador)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Comprador>() {
        override fun areItemsTheSame(oldItem: Comprador, newItem: Comprador) = oldItem.idComprador == newItem.idComprador
        override fun areContentsTheSame(oldItem: Comprador, newItem: Comprador) = oldItem == newItem
    }
}
