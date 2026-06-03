package com.emiliano.lechapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.emiliano.lechapp.databinding.ItemAnimalBinding
import java.util.Locale

class AnimalAdapter(private val onDeleteClick: (AnimalLote) -> Unit) : ListAdapter<AnimalConProduccion, AnimalAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAnimalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onDeleteClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemAnimalBinding,
        private val onDeleteClick: (AnimalLote) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AnimalConProduccion) {
            val animal = item.animal
            binding.tvNombre.text = animal.identificador
            binding.tvRazaLote.text = "${animal.raza ?: "Sin raza"} - ${if (animal.esLoteGeneral) "Lote" else "Individual"}"
            binding.tvProduccionTotal.text = String.format(Locale.getDefault(), "Producción histórica: %.1f Litros", item.totalLitros)
            
            // Ícono visual según si es lote o individual
            binding.imgAnimalType.setImageResource(
                if (animal.esLoteGeneral) android.R.drawable.ic_menu_agenda else android.R.drawable.ic_menu_gallery
            )

            binding.btnBorrarAnimal.setOnClickListener {
                onDeleteClick(animal)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<AnimalConProduccion>() {
        override fun areItemsTheSame(oldItem: AnimalConProduccion, newItem: AnimalConProduccion) = 
            oldItem.animal.idAnimal == newItem.animal.idAnimal
            
        override fun areContentsTheSame(oldItem: AnimalConProduccion, newItem: AnimalConProduccion) = 
            oldItem == newItem
    }
}
