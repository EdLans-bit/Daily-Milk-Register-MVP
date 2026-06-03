package com.emiliano.lechapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.emiliano.lechapp.databinding.ItemVacaRankingBinding
import java.util.Locale

class RankingAdapter(private val onVacaClick: (AnimalLote) -> Unit) : 
    ListAdapter<AnimalConProduccion, RankingAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemVacaRankingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onVacaClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemVacaRankingBinding,
        private val onVacaClick: (AnimalLote) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AnimalConProduccion) {
            val animal = item.animal
            binding.tvNombreVacaItem.text = animal.identificador
            binding.tvEstadoVacaItem.text = String.format(Locale.getDefault(), "Promedio: %.1f L", item.promedioLitros)
            
            // Mostramos el total histórico
            binding.tvLitrosVacaItem.text = String.format(Locale.getDefault(), "%.1f L", item.totalLitros)
            
            binding.root.setOnClickListener { onVacaClick(animal) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<AnimalConProduccion>() {
        override fun areItemsTheSame(oldItem: AnimalConProduccion, newItem: AnimalConProduccion): Boolean {
            return oldItem.animal.idAnimal == newItem.animal.idAnimal
        }
        override fun areContentsTheSame(oldItem: AnimalConProduccion, newItem: AnimalConProduccion): Boolean {
            return oldItem == newItem
        }
    }
}
