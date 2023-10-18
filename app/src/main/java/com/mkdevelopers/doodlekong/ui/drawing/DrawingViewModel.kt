package com.mkdevelopers.doodlekong.ui.drawing

import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.mkdevelopers.doodlekong.R
import com.mkdevelopers.doodlekong.data.remote.ws.DrawingApi
import com.mkdevelopers.doodlekong.util.DispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class DrawingViewModel @Inject constructor(
    private val drawingApi: DrawingApi,
    private val dispatchers: DispatcherProvider,
    private val gson: Gson
) : ViewModel() {

    private val _selectedColorButtonId = MutableStateFlow(R.id.rbBlack)
    val selectedColorButtonId = _selectedColorButtonId.asStateFlow()

    fun checkRadioButton(id: Int) {
        _selectedColorButtonId.value = id
    }
}