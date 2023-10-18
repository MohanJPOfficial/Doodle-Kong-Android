package com.mkdevelopers.doodlekong.data.remote.ws.model

import com.mkdevelopers.doodlekong.util.Constants

data class GameError(
    val errorType: Int
): BaseModel(Constants.TYPE_GAME_ERROR) {

    companion object {
        const val ERROR_ROOM_NOT_FOUND = 0
    }
}
