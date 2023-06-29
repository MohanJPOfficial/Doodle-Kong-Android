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
class UsernameViewModel @Inject constructor(
    private val dispatchers: DispatcherProvider
): ViewModel() {

    private val _setupEvent = MutableSharedFlow<SetupEvent>()
    val setupEvent: SharedFlow<SetupEvent> = _setupEvent

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

    //Ui events
    sealed class SetupEvent {
        object InputEmptyError : SetupEvent()
        object InputShortError : SetupEvent()
        object InputTooLongError : SetupEvent()

        data class NavigateToSelectRoomEvent(val username: String) : SetupEvent()
    }
}