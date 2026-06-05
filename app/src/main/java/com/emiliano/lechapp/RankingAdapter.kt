package com.emiliano.lechapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.emiliano.lechapp.databinding.ItemRankingVacaBinding
import java.util.Locale

class RankingAdapter : ListAdapter<AnimalConProduccion, RankingAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRankingVacaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position + 1)
    }

    class ViewHolder(private val binding: ItemRankingVacaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AnimalConProduccion, rank: Int) {
            val animal = item.animal
            binding.tvRankNumber.text = rank.toString()
            binding.tvVacaNombre.text = animal.identificador
            binding.tvVacaProduccion.text = "${String.format(Locale.getDefault(), "%.1f", item.totalLitros)} Litros"
            
            // Especial para el Top 1
            if (rank == 1) {
                binding.ivTrophy.visibility = View.VISIBLE
                binding.viewRankBg.backgroundTintList = binding.root.context.getColorStateList(R.color.bg_badge)
            } else {
                binding.ivTrophy.visibility = View.GONE
                binding.viewRankBg.backgroundTintList = null
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
