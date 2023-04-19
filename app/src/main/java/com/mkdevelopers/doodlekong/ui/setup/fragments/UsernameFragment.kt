package com.mkdevelopers.doodlekong.ui.setup.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.whenStarted
import androidx.navigation.fragment.findNavController
import com.mkdevelopers.doodlekong.R
import com.mkdevelopers.doodlekong.databinding.FragmentUsernameBinding
import com.mkdevelopers.doodlekong.ui.setup.SetupViewModel
import com.mkdevelopers.doodlekong.util.Constants
import com.mkdevelopers.doodlekong.util.navigateSafely
import com.mkdevelopers.doodlekong.util.showSnackBar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UsernameFragment : Fragment(R.layout.fragment_username) {

    private var _binding: FragmentUsernameBinding? = null
    private val binding: FragmentUsernameBinding
        get() = _binding!!

    private val viewModel: SetupViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentUsernameBinding.bind(view)

        listenToEvents()

        binding.btnNext.setOnClickListener {
            viewModel.validateUsernameAndNavigateToSelectRoom(
                binding.etUsername.text.toString()
            )
        }
    }

    private fun listenToEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                /**
                 * setupEvent flow collects duplicate events
                 * that's why nav controller gave illegal state exception
                 * need to fix this bug.
                 * update : fixed by using viewLifecycleOwner
                 */

                viewModel.setupEvent.collect { event ->

                    when(event) {
                        is SetupViewModel.SetupEvent.NavigateToSelectRoomEvent -> {
                            findNavController().navigate(
                                    directions = UsernameFragmentDirections.actionUsernameFragmentToSelectRoomFragment(
                                    /*username = */event.username
                                )
                            )
                            println("NavigateToSelectRoomEvent >> fired")
                            /*findNavController().navigate(
                                resId = R.id.action_usernameFragment_to_selectRoomFragment,
                                args = Bundle().apply { putString("username", event.username) }
                            )*/
                        }
                        SetupViewModel.SetupEvent.InputEmptyError -> {
                            showSnackBar(R.string.error_field_empty)
                        }
                        SetupViewModel.SetupEvent.InputShortError -> {
                            showSnackBar(getString(R.string.error_username_too_short, Constants.MIN_USERNAME_LENGTH))
                        }
                        SetupViewModel.SetupEvent.InputTooLongError -> {
                            showSnackBar(getString(R.string.error_username_too_long, Constants.MAX_USERNAME_LENGTH))
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}