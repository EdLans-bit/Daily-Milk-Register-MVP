package com.emiliano.lechapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.emiliano.lechapp.databinding.ItemEntregaBinding
import java.text.SimpleDateFormat
import java.util.*

class EntregaAdapter(private val onDeleteClick: (RegistroLeche) -> Unit) : ListAdapter<RegistroLeche, EntregaAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEntregaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onDeleteClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemEntregaBinding,
        private val onDeleteClick: (RegistroLeche) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(registro: RegistroLeche) {
            binding.tvComprador.text = registro.comprador
            binding.tvLitros.text = "${registro.litros} Litros"
            
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            binding.tvFecha.text = sdf.format(Date(registro.fecha))

            binding.btnBorrarItem.setOnClickListener {
                onDeleteClick(registro)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<RegistroLeche>() {
        override fun areItemsTheSame(oldItem: RegistroLeche, newItem: RegistroLeche) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: RegistroLeche, newItem: RegistroLeche) = oldItem == newItem
    }
}