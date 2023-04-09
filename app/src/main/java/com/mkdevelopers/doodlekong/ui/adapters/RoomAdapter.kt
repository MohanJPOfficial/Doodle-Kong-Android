package com.mkdevelopers.doodlekong.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.mkdevelopers.doodlekong.data.remote.ws.Room
import com.mkdevelopers.doodlekong.databinding.ItemRoomBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RoomAdapter @Inject constructor() : RecyclerView.Adapter<RoomAdapter.RoomViewHolder>(){

    private var onRoomClickListener: ((Room) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        return RoomViewHolder(
            ItemRoomBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        val room = rooms[position]
        holder.bindData(room)
    }

    override fun getItemCount(): Int {
        return rooms.size
    }

    inner class RoomViewHolder(private val binding: ItemRoomBinding): RecyclerView.ViewHolder(binding.root) {
        fun bindData(room: Room) {
            binding.apply {
                tvRoomName.text = room.name
                val playerCountText = "${room.playerCount} / ${room.maxPlayers}"
                tvRoomPersonCount.text = playerCountText

                itemView.setOnClickListener {
                    onRoomClickListener?.invoke(room)
                }
            }
        }
    }

    var rooms = listOf<Room>()
        private set

    suspend fun updateDataset(newDataset: List<Room>) = withContext(Dispatchers.Default) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int {
                return rooms.size
            }

            override fun getNewListSize(): Int {
                return newDataset.size
            }

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return rooms[oldItemPosition] == newDataset[newItemPosition]
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return rooms[oldItemPosition] == newDataset[newItemPosition]
            }
        })
        withContext(Dispatchers.Main) {
            rooms = newDataset
            diff.dispatchUpdatesTo(this@RoomAdapter)
        }
    }

    fun setOnRoomClickListener(listener: (Room) -> Unit) {
        onRoomClickListener = listener
    }
}