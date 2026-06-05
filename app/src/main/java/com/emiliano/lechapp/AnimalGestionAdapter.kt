package com.emiliano.lechapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.emiliano.lechapp.databinding.ItemAnimalGestionBinding
import java.util.Locale

class AnimalGestionAdapter(
    private val onItemClick: (AnimalConProduccion) -> Unit,
    private val onDeleteClick: (AnimalLote) -> Unit
) : ListAdapter<AnimalConProduccion, AnimalGestionAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAnimalGestionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick, onDeleteClick)
    }

    class ViewHolder(private val binding: ItemAnimalGestionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AnimalConProduccion, onItemClick: (AnimalConProduccion) -> Unit, onDeleteClick: (AnimalLote) -> Unit) {
            val animal = item.animal
            binding.tvNombre.text = animal.identificador
            binding.tvSubtitulo.text = "${animal.raza} - ${if (animal.esLoteGeneral) "Lote" else "Individual"}"
            binding.tvProduccionHistorial.text = "Producción histórica: ${String.format(Locale.getDefault(), "%.1f", item.totalLitros)} Litros"
            
            binding.btnEliminar.setOnClickListener { onDeleteClick(animal) }
            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<AnimalConProduccion>() {
        override fun areItemsTheSame(oldItem: AnimalConProduccion, newItem: AnimalConProduccion) =
            oldItem.animal.idAnimal == newItem.animal.idAnimal
        override fun areContentsTheSame(oldItem: AnimalConProduccion, newItem: AnimalConProduccion) =
            oldItem == newItem
    }
}
