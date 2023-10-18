package com.mkdevelopers.doodlekong.data.remote.ws.model

import com.mkdevelopers.doodlekong.util.Constants

data class ChatMessage(
    val from: String,
    val roomName: String,
    val message: String,
    val timestamp: Long
): BaseModel(Constants.TYPE_CHAT_MESSAGE)
