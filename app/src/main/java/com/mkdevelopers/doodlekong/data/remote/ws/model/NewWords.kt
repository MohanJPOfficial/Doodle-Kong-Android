package com.mkdevelopers.doodlekong.data.remote.ws.model

import com.mkdevelopers.doodlekong.util.Constants

data class NewWords(
    val newWords: List<String>
): BaseModel(Constants.TYPE_NEW_WORDS)