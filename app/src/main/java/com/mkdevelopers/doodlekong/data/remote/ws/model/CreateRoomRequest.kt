package com.mkdevelopers.doodlekong.data.remote.ws.model

data class CreateRoomRequest(
    val name: String,
    val maxPlayers: Int
)