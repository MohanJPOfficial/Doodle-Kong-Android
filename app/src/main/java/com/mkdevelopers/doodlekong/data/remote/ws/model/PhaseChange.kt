package com.mkdevelopers.doodlekong.data.remote.ws.model

import com.mkdevelopers.doodlekong.data.remote.ws.Room
import com.mkdevelopers.doodlekong.util.Constants

data class PhaseChange(
    var phase: Room.Phase?,
    var time: Long,
    val drawingPlayer: String? = null
): BaseModel(Constants.TYPE_PHASE_CHANGE)