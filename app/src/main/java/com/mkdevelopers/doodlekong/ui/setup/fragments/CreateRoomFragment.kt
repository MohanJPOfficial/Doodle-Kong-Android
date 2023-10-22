package com.mkdevelopers.doodlekong.ui.setup.fragments

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mkdevelopers.doodlekong.R
import com.mkdevelopers.doodlekong.data.remote.ws.Room
import com.mkdevelopers.doodlekong.databinding.FragmentCreateRoomBinding
import com.mkdevelopers.doodlekong.ui.setup.CreateRoomViewModel
import com.mkdevelopers.doodlekong.util.Constants
import com.mkdevelopers.doodlekong.util.hideKeyboard
import com.mkdevelopers.doodlekong.util.showSnackBar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CreateRoomFragment : Fragment(R.layout.fragment_create_room) {

    private var _binding: FragmentCreateRoomBinding? = null
    private val binding: FragmentCreateRoomBinding
        get() = _binding!!

    private val viewModel: CreateRoomViewModel by viewModels()
    private val args: CreateRoomFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCreateRoomBinding.bind(view)
        requireActivity().window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        setupRoomSizeSpinner()
        listenToEvents()

        binding.btnCreateRoom.setOnClickListener {
            binding.createRoomProgressBar.isVisible = true
            viewModel.createRoom(
                Room(
                    binding.etRoomName.text.toString(),
                    binding.tvMaxPersons.text.toString().toInt()
                )
            )
            requireActivity().hideKeyboard(binding.root)
        }
    }

    private fun listenToEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.setupEvent.collect { event ->
                    when(event) {
                        is CreateRoomViewModel.SetupEvent.CreateRoomEvent -> {
                            viewModel.joinRoom(args.username,  event.room.name)
                        }
                        is CreateRoomViewModel.SetupEvent.InputEmptyError -> {
                            binding.createRoomProgressBar.isVisible = false
                            showSnackBar(R.string.error_field_empty)
                        }
                        is CreateRoomViewModel.SetupEvent.InputShortError -> {
                            binding.createRoomProgressBar.isVisible = false
                            showSnackBar(getString(R.string.error_room_name_too_short, Constants.MIN_ROOM_NAME_LENGTH))
                        }
                        is CreateRoomViewModel.SetupEvent.InputTooLongError -> {
                            binding.createRoomProgressBar.isVisible = false
                            showSnackBar(getString(R.string.error_room_name_too_long, Constants.MAX_ROOM_NAME_LENGTH))
                        }
                        is CreateRoomViewModel.SetupEvent.CreateRoomErrorEvent -> {
                            binding.createRoomProgressBar.isVisible = false
                            showSnackBar(event.error)
                        }
                        is CreateRoomViewModel.SetupEvent.JoinRoomEvent -> {
                            binding.createRoomProgressBar.isVisible = false
                            findNavController().navigate(
                                directions = CreateRoomFragmentDirections.actionCreateRoomFragmentToDrawingActivity(
                                    /* username = */ args.username,
                                    /* roomName = */ event.roomName
                                )
                            )
                        }
                        is CreateRoomViewModel.SetupEvent.JoinRoomErrorEvent -> {
                            binding.createRoomProgressBar.isVisible = false
                            showSnackBar(event.error)
                        }
                    }
                }
            }
        }
    }

    private fun setupRoomSizeSpinner() {
        val roomSizes = resources.getStringArray(R.array.room_size_array)
        val adapter = ArrayAdapter(requireContext(), R.layout.textview_room_size, roomSizes)
        binding.tvMaxPersons.setAdapter(adapter)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}