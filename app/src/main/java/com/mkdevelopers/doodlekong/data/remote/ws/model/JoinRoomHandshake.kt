package com.mkdevelopers.doodlekong.data.remote.ws.model

import com.mkdevelopers.doodlekong.util.Constants

data class JoinRoomHandshake(
    val username: String,
    val roomName: String,
    val clientId: String
): BaseModel(Constants.TYPE_JOIN_ROOM_HANDSHAKE)