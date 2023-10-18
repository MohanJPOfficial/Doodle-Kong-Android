package com.mkdevelopers.doodlekong.data.remote.ws.model

import com.mkdevelopers.doodlekong.util.Constants

data class GameState(
    val drawingPlayer: String,
    val word: String
): BaseModel(Constants.TYPE_GAME_STATE)