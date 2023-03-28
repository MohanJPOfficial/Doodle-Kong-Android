package com.mkdevelopers.doodlekong.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mkdevelopers.doodlekong.data.remote.ws.Room
import com.mkdevelopers.doodlekong.repository.SetupRepository
import com.mkdevelopers.doodlekong.util.Constants
import com.mkdevelopers.doodlekong.util.DispatcherProvider
import com.mkdevelopers.doodlekong.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val repository: SetupRepository,
    private val dispatchers: DispatcherProvider
): ViewModel() {

    private val _setupEvent = MutableSharedFlow<SetupEvent>()
    val setupEvent: SharedFlow<SetupEvent> = _setupEvent

    private val _rooms = MutableStateFlow<SetupEvent>(SetupEvent.GetRoomEmptyEvent)
    val rooms: StateFlow<SetupEvent> = _rooms

    fun validateUsernameAndNavigateToSelectRoom(username: String) {
        viewModelScope.launch(dispatchers.main) {
            val trimmedUsername = username.trim()
            when {
                trimmedUsername.isEmpty() -> {
                    _setupEvent.emit(SetupEvent.InputEmptyError)
                }
                trimmedUsername.length < Constants.MIN_USERNAME_LENGTH -> {
                    _setupEvent.emit(SetupEvent.InputShortError)
                }
                trimmedUsername.length > Constants.MAX_USERNAME_LENGTH -> {
                    _setupEvent.emit(SetupEvent.InputTooLongError)
                }
                else -> {
                    _setupEvent.emit(SetupEvent.NavigateToSelectRoomEvent(username))
                }
            }
        }
    }

    fun createRoom(room: Room) {
        viewModelScope.launch(dispatchers.main) {
            val trimmedRoomName = room.name.trim()
            when {
                trimmedRoomName.isEmpty() -> {
                    _setupEvent.emit(SetupEvent.InputEmptyError)
                }
                trimmedRoomName.length < Constants.MIN_ROOM_NAME_LENGTH -> {
                    _setupEvent.emit(SetupEvent.InputShortError)
                }
                trimmedRoomName.length > Constants.MAX_ROOM_NAME_LENGTH -> {
                    _setupEvent.emit(SetupEvent.InputTooLongError)
                }
                else -> {
                    val result = repository.createRoom(room)
                    if(result is Resource.Success) {
                        _setupEvent.emit(SetupEvent.CreateRoomEvent(room))
                    } else {
                        _setupEvent.emit(
                            SetupEvent.CreateRoomErrorEvent(
                                result.message ?: return@launch
                            )
                        )
                    }
                }
            }
        }
    }

    fun getRooms(searchQuery: String) {
        _rooms.value = SetupEvent.GetRoomLoadingEvent
        viewModelScope.launch(dispatchers.main) {
            val result = repository.getRooms(searchQuery)
            if(result is Resource.Success) {
                _rooms.value = SetupEvent.GetRoomEvent(result.data ?: return@launch)
            } else {
                _setupEvent.emit(SetupEvent.GetRoomErrorEvent(result.message ?: return@launch))
            }
        }
    }

    fun joinRoom(username: String, roomName: String) {
        viewModelScope.launch(dispatchers.main) {
            val result = repository.joinRoom(username, roomName)
            if(result is Resource.Success) {
                _setupEvent.emit(SetupEvent.JoinRoomEvent(roomName))
            } else {
                _setupEvent.emit(SetupEvent.JoinRoomErrorEvent(result.message ?: return@launch))
            }
        }
    }

    //Ui events
    sealed class SetupEvent {
        object InputEmptyError : SetupEvent()
        object InputShortError : SetupEvent()
        object InputTooLongError : SetupEvent()

        data class CreateRoomEvent(val room: Room) : SetupEvent()
        data class CreateRoomErrorEvent(val error: String) : SetupEvent()

        data class NavigateToSelectRoomEvent(val username: String) : SetupEvent()

        data class GetRoomEvent(val rooms: List<Room>) : SetupEvent()
        data class GetRoomErrorEvent(val error: String) : SetupEvent()
        object GetRoomLoadingEvent : SetupEvent()
        object GetRoomEmptyEvent : SetupEvent()

        data class JoinRoomEvent(val roomName: String) : SetupEvent()
        data class JoinRoomErrorEvent(val error: String) : SetupEvent()
    }
}