package com.mkdevelopers.doodlekong.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.mkdevelopers.doodlekong.data.remote.ws.model.PlayerData
import com.mkdevelopers.doodlekong.databinding.ItemPlayerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PlayerAdapter @Inject constructor() : RecyclerView.Adapter<PlayerAdapter.PlayerViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        return PlayerViewHolder(
            ItemPlayerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        val room = players[position]
        holder.bindData(room)
    }

    override fun getItemCount(): Int {
        return players.size
    }

    inner class PlayerViewHolder(private val binding: ItemPlayerBinding): RecyclerView.ViewHolder(binding.root) {
        fun bindData(player: PlayerData) {
            binding.apply {
                val playerRankText = "${player.rank}. "
                tvRank.text = playerRankText
                tvScore.text = player.score.toString()
                tvUsername.text = player.username
                ivPencil.isVisible = player.isDrawing
            }
        }
    }

    var players = listOf<PlayerData>()
        private set

    suspend fun updateDataset(newDataset: List<PlayerData>) = withContext(Dispatchers.Default) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int {
                return players.size
            }

            override fun getNewListSize(): Int {
                return newDataset.size
            }

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return players[oldItemPosition] == newDataset[newItemPosition]
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return players[oldItemPosition] == newDataset[newItemPosition]
            }
        })
        withContext(Dispatchers.Main) {
            players = newDataset
            diff.dispatchUpdatesTo(this@PlayerAdapter)
        }
    }
}