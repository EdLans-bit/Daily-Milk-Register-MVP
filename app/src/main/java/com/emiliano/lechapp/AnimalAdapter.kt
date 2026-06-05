package com.emiliano.lechapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.emiliano.lechapp.databinding.ItemVacaRankingCrudBinding
import java.util.Locale

class AnimalAdapter(
    private val onItemClick: (AnimalConProduccion) -> Unit,
    private val onEditClick: ((AnimalLote) -> Unit)? = null,
    private val onDeleteClick: ((AnimalLote) -> Unit)? = null
) : ListAdapter<AnimalConProduccion, AnimalAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemVacaRankingCrudBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onItemClick, onEditClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position + 1)
    }

    class ViewHolder(
        private val binding: ItemVacaRankingCrudBinding,
        private val onItemClick: (AnimalConProduccion) -> Unit,
        private val onEditClick: ((AnimalLote) -> Unit)?,
        private val onDeleteClick: ((AnimalLote) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AnimalConProduccion, rank: Int) {
            val animal = item.animal
            binding.tvNombreVaca.text = String.format(Locale.getDefault(), "Vaca %s: %.1f L hoy / %.1f L total", 
                animal.identificador, item.litrosHoy, item.totalLitros)
            binding.tvRankingBadge.text = rank.toString()
            
            if (onEditClick != null) {
                binding.btnEditarVaca.visibility = View.VISIBLE
                binding.btnEditarVaca.setOnClickListener { onEditClick.invoke(animal) }
            } else {
                binding.btnEditarVaca.visibility = View.GONE
            }

            if (onDeleteClick != null) {
                binding.btnEliminarVaca.visibility = View.VISIBLE
                binding.btnEliminarVaca.setOnClickListener { onDeleteClick.invoke(animal) }
            } else {
                binding.btnEliminarVaca.visibility = View.GONE
            }

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
