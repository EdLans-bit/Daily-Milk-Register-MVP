package com.emiliano.lechapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.emiliano.lechapp.databinding.ItemVacaSeleccionBinding

class VacaSelectionAdapter(
    private val onVacaSelected: (AnimalConProduccion) -> Unit
) : ListAdapter<AnimalConProduccion, VacaSelectionAdapter.ViewHolder>(DiffCallback()) {

    private var selectedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemVacaSeleccionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, position == selectedPosition)
        holder.itemView.setOnClickListener {
            val previous = selectedPosition
            selectedPosition = holder.adapterPosition
            notifyItemChanged(previous)
            notifyItemChanged(selectedPosition)
            onVacaSelected(item)
        }
    }

    fun selectFirst() {
        if (itemCount > 0 && selectedPosition == -1) {
            selectedPosition = 0
            notifyItemChanged(0)
            onVacaSelected(getItem(0))
        }
    }

    class ViewHolder(private val binding: ItemVacaSeleccionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AnimalConProduccion, isSelected: Boolean) {
            binding.tvNombreVaca.text = item.animal.identificador
            binding.cardVaca.strokeColor = if (isSelected) {
                binding.root.context.getColor(R.color.primary_green)
            } else {
                binding.root.context.getColor(R.color.border_color)
            }
            binding.cardVaca.setCardBackgroundColor(if (isSelected) {
                binding.root.context.getColor(R.color.bg_badge)
            } else {
                binding.root.context.getColor(R.color.surface)
            })
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<AnimalConProduccion>() {
        override fun areItemsTheSame(oldItem: AnimalConProduccion, newItem: AnimalConProduccion) =
            oldItem.animal.idAnimal == newItem.animal.idAnimal
        override fun areContentsTheSame(oldItem: AnimalConProduccion, newItem: AnimalConProduccion) =
            oldItem == newItem
    }
}
