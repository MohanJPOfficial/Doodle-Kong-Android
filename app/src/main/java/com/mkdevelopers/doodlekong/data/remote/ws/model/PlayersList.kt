package com.mkdevelopers.doodlekong.data.remote.ws.model

import com.mkdevelopers.doodlekong.util.Constants

data class PlayersList(
    val players: List<PlayerData>
): BaseModel(Constants.TYPE_PLAYERS_LIST)