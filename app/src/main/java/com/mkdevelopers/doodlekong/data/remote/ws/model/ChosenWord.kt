package com.mkdevelopers.doodlekong.data.remote.ws.model

import com.mkdevelopers.doodlekong.util.Constants

data class ChosenWord(
    val chosenWord: String,
    val roomName: String
): BaseModel(Constants.TYPE_CHOSEN_WORD)
