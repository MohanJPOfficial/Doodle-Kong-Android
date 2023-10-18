package com.mkdevelopers.doodlekong.data.remote.ws.model

import com.mkdevelopers.doodlekong.util.Constants

data class DrawAction(
    val action: String
): BaseModel(Constants.TYPE_DRAW_ACTION) {

    companion object {
        const val ACTION_UNDO = "ACTION_UNDO"
    }
}