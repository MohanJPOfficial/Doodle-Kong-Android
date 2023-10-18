package com.mkdevelopers.doodlekong.data.remote.ws.model

import com.mkdevelopers.doodlekong.util.Constants

data class RoundDrawInfo(
    val data: List<String>
): BaseModel(Constants.TYPE_CUR_ROUND_DRAW_INFO)