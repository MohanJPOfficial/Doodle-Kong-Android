package com.mkdevelopers.doodlekong.ui.setup.fragments

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.mkdevelopers.doodlekong.R
import com.mkdevelopers.doodlekong.databinding.FragmentSelectRoomBinding
import com.mkdevelopers.doodlekong.ui.adapters.RoomAdapter
import com.mkdevelopers.doodlekong.ui.setup.SelectRoomViewModel
import com.mkdevelopers.doodlekong.util.Constants
import com.mkdevelopers.doodlekong.util.showSnackBar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SelectRoomFragment : Fragment(R.layout.fragment_select_room) {

    private var _binding: FragmentSelectRoomBinding? = null
    private val binding: FragmentSelectRoomBinding
        get() = _binding!!

    private val viewModel: SelectRoomViewModel by viewModels()

    private val args: SelectRoomFragmentArgs by navArgs()

    @Inject
    lateinit var roomAdapter: RoomAdapter

    private var updateRoomsJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSelectRoomBinding.bind(view)

        setupRecyclerView()
        subscribeToObservers()
        listenToEvents()

        viewModel.getRooms("")

        var searchJob: Job? = null
        binding.etRoomName.doAfterTextChanged {
            searchJob?.cancel()
            searchJob = lifecycleScope.launch {
                delay(Constants.SEARCH_DELAY)
                viewModel.getRooms(it.toString())
            }
        }

        binding.ibReload.setOnClickListener {

            binding.roomsProgressBar.isVisible = true
            binding.ivNoRoomsFound.isVisible = false
            binding.tvNoRoomsFound.isVisible = false

            viewModel.getRooms(binding.etRoomName.text.toString())
        }

        binding.btnCreateRoom.setOnClickListener {
            findNavController().navigate(
                SelectRoomFragmentDirections.actionSelectRoomFragmentToCreateRoomFragment(
                    /* username = */ args.username
                )
            )
        }

        roomAdapter.setOnRoomClickListener {
            viewModel.joinRoom(
                username = args.username,
                roomName = it.name
            )
        }
    }

    private fun listenToEvents() = lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.setupEvent.collect { event ->
                when(event) {
                    is SelectRoomViewModel.SetupEvent.JoinRoomEvent -> {
                        findNavController().navigate(
                            directions = SelectRoomFragmentDirections.actionSelectRoomFragmentToDrawingActivity(
                                /* username = */ args.username,
                                /* roomName = */ event.roomName
                            )
                        )
                    }
                    is SelectRoomViewModel.SetupEvent.JoinRoomErrorEvent -> {
                        showSnackBar(event.error)
                    }
                    is SelectRoomViewModel.SetupEvent.GetRoomErrorEvent -> {
                        binding.apply {
                            roomsProgressBar.isVisible = false
                            tvNoRoomsFound.isVisible = false
                            ivNoRoomsFound.isVisible = false
                        }
                        showSnackBar(event.error)
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun subscribeToObservers() = lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.rooms.collect { event ->
                when(event) {
                    SelectRoomViewModel.SetupEvent.GetRoomLoadingEvent -> {
                        binding.roomsProgressBar.isVisible = true
                    }
                    is SelectRoomViewModel.SetupEvent.GetRoomEvent -> {
                        binding.roomsProgressBar.isVisible = false
                        val isRoomEmpty = event.rooms.isEmpty()
                        binding.tvNoRoomsFound.isVisible = isRoomEmpty
                        binding.ivNoRoomsFound.isVisible = isRoomEmpty

                        updateRoomsJob?.cancel()
                        updateRoomsJob = lifecycleScope.launch {
                            roomAdapter.updateDataset(event.rooms)
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun setupRecyclerView() {
        binding.rvRooms.apply {
            adapter = roomAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}