package com.mkdevelopers.doodlekong.ui.drawing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.mkdevelopers.doodlekong.R
import com.mkdevelopers.doodlekong.data.remote.ws.DrawingApi
import com.mkdevelopers.doodlekong.data.remote.ws.model.Announcement
import com.mkdevelopers.doodlekong.data.remote.ws.model.BaseModel
import com.mkdevelopers.doodlekong.data.remote.ws.model.ChatMessage
import com.mkdevelopers.doodlekong.data.remote.ws.model.ChosenWord
import com.mkdevelopers.doodlekong.data.remote.ws.model.DrawAction
import com.mkdevelopers.doodlekong.data.remote.ws.model.DrawData
import com.mkdevelopers.doodlekong.data.remote.ws.model.GameError
import com.mkdevelopers.doodlekong.data.remote.ws.model.GameState
import com.mkdevelopers.doodlekong.data.remote.ws.model.NewWords
import com.mkdevelopers.doodlekong.data.remote.ws.model.Ping
import com.mkdevelopers.doodlekong.data.remote.ws.model.RoundDrawInfo
import com.mkdevelopers.doodlekong.util.DispatcherProvider
import com.tinder.scarlet.WebSocket
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DrawingViewModel @Inject constructor(
    private val drawingApi: DrawingApi,
    private val dispatchers: DispatcherProvider,
    private val gson: Gson
) : ViewModel() {

    private val _selectedColorButtonId = MutableStateFlow(R.id.rbBlack)
    val selectedColorButtonId = _selectedColorButtonId.asStateFlow()

    private val _connectionProgressBarVisible = MutableStateFlow(true)
    val connectionProgressBarVisible = _connectionProgressBarVisible.asStateFlow()

    private val _chooseWordOverlayVisible = MutableStateFlow(false)
    val chooseWordOverlayVisible = _chooseWordOverlayVisible.asStateFlow()

    private val connectionEventChannel = Channel<WebSocket.Event>()
    val connectionEvent = connectionEventChannel.receiveAsFlow().flowOn(dispatchers.io)

    private val socketEventChannel = Channel<SocketEvent>()
    val socketEvent = socketEventChannel.receiveAsFlow().flowOn(dispatchers.io)

    init {
        observeBaseModels()
        observeEvents()
    }

    fun setChooseWordOverlayVisibility(isVisible: Boolean) {
        _chooseWordOverlayVisible.value = isVisible
    }

    fun setConnectionProgressBarVisibility(isVisible: Boolean) {
        _connectionProgressBarVisible.value = isVisible
    }

    fun checkRadioButton(id: Int) {
        _selectedColorButtonId.value = id
    }

    private fun observeEvents() = viewModelScope.launch(dispatchers.io) {
        drawingApi.observeEvents().collect { event ->
            connectionEventChannel.send(event)
        }
    }

    private fun observeBaseModels() = viewModelScope.launch(dispatchers.io) {
        drawingApi.observeBaseModels().collect { data ->
            when(data) {
                is DrawData -> {
                    socketEventChannel.send(SocketEvent.DrawDataEvent(data))
                }
                is DrawAction -> {
                    when(data.action) {
                        DrawAction.ACTION_UNDO -> socketEventChannel.send(SocketEvent.UndoEvent)
                    }
                }
                is GameError -> socketEventChannel.send(SocketEvent.GameErrorEvent(data))
                is Ping -> sendBaseModel(Ping())
            }
        }
    }

    fun sendBaseModel(data: BaseModel) = viewModelScope.launch(dispatchers.io) {
        drawingApi.sendBaseModel(data)
    }
}

sealed class SocketEvent {
    data class ChatMessageEvent(val data: ChatMessage): SocketEvent()
    data class AnnouncementEvent(val data: Announcement): SocketEvent()
    data class GameStateEvent(val data: GameState): SocketEvent()
    data class DrawDataEvent(val data: DrawData): SocketEvent()
    data class NewWordsEvent(val data: NewWords): SocketEvent()
    data class ChosenWordEvent(val data: ChosenWord): SocketEvent()
    data class GameErrorEvent(val data: GameError): SocketEvent()
    data class RoundDrawInfoEvent(val data: RoundDrawInfo): SocketEvent()
    object UndoEvent: SocketEvent()
}