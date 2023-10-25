package com.mkdevelopers.doodlekong.ui.drawing

import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.mkdevelopers.doodlekong.R
import com.mkdevelopers.doodlekong.data.remote.ws.Room
import com.mkdevelopers.doodlekong.data.remote.ws.model.BaseModel
import com.mkdevelopers.doodlekong.data.remote.ws.model.ChatMessage
import com.mkdevelopers.doodlekong.data.remote.ws.model.DrawAction
import com.mkdevelopers.doodlekong.data.remote.ws.model.GameError
import com.mkdevelopers.doodlekong.data.remote.ws.model.JoinRoomHandshake
import com.mkdevelopers.doodlekong.data.remote.ws.model.PhaseChange
import com.mkdevelopers.doodlekong.data.remote.ws.model.PlayerData
import com.mkdevelopers.doodlekong.databinding.ActivityDrawingBinding
import com.mkdevelopers.doodlekong.ui.adapters.ChatMessageAdapter
import com.mkdevelopers.doodlekong.ui.adapters.PlayerAdapter
import com.mkdevelopers.doodlekong.util.Constants
import com.mkdevelopers.doodlekong.util.hideKeyboard
import com.tinder.scarlet.WebSocket
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DrawingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDrawingBinding

    private val viewModel: DrawingViewModel by viewModels()

    private val args: DrawingActivityArgs by navArgs()

    @Inject
    lateinit var clientId: String

    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var rvPlayers: RecyclerView

    @Inject
    lateinit var playerAdapter: PlayerAdapter

    private lateinit var chatMessageAdapter: ChatMessageAdapter

    private var updateChatJob: Job? = null
    private var updatePlayersJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDrawingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        subscribeToUiStateUpdates()
        listenToConnectionEvents()
        listenToSocketEvents()
        setupRecyclerView()

        toggle = ActionBarDrawerToggle(this, binding.root, R.string.open, R.string.close)
        toggle.syncState()

        binding.drawingView.roomName = args.roomName

        val header = layoutInflater.inflate(R.layout.nav_drawer_header, binding.navView)
        rvPlayers = header.findViewById(R.id.rvPlayers)
        binding.root.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

        rvPlayers.apply {
            adapter = playerAdapter
            layoutManager = LinearLayoutManager(this@DrawingActivity)
        }

        binding.ibPlayers.setOnClickListener {
            binding.root.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            binding.root.openDrawer(GravityCompat.START)
        }

        binding.root.addDrawerListener(object: DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) = Unit

            override fun onDrawerOpened(drawerView: View) = Unit

            override fun onDrawerClosed(drawerView: View) {
                binding.root.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            }

            override fun onDrawerStateChanged(newState: Int) = Unit
        })

        binding.ibClearText.setOnClickListener {
            binding.etMessage.text?.clear()
        }

        binding.ibSend.setOnClickListener {
            viewModel.sendChatMessage(
                ChatMessage(
                    from = args.username,
                    roomName = args.roomName,
                    message = binding.etMessage.text.toString(),
                    timestamp = System.currentTimeMillis()
                )
            )
            binding.etMessage.text?.clear()
            hideKeyboard(binding.root)
        }

        binding.ibUndo.setOnClickListener {
            if(binding.drawingView.isUserDrawing) {
                binding.drawingView.undo()
                viewModel.sendBaseModel(
                    DrawAction(DrawAction.ACTION_UNDO)
                )
            }
        }

        binding.colorGroup.setOnCheckedChangeListener { _, checkedId ->
            viewModel.checkRadioButton(checkedId)
        }

        binding.drawingView.setPathDataChangedListener {
            viewModel.setPathData(it)
        }

        binding.drawingView.setOnDrawListener {

            if(binding.drawingView.isUserDrawing) {
                viewModel.sendBaseModel(it)
            }
        }
    }

    private fun setColorGroupVisibility(isVisible: Boolean) {
        binding.apply {
            colorGroup.isVisible = isVisible
            ibUndo.isVisible = isVisible
        }
    }

    private fun setMessageInputVisibility(isVisible: Boolean) {
        binding.apply {
            tilMessage.isVisible = isVisible
            ibSend.isVisible = isVisible
            ibClearText.isVisible = isVisible
        }
    }

    private fun selectColor(color: Int) {
        binding.drawingView.setColor(color)
        binding.drawingView.setThickness(Constants.DEFAULT_PAINT_THICKNESS)
    }

    private fun subscribeToUiStateUpdates() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.chat.collect { chat ->
                        if(chatMessageAdapter.chatObjects.isEmpty()) {
                            updateChatMessageList(chat)
                        }
                    }
                }

                launch {
                    viewModel.newWords.collect {
                        val newWords = it.newWords

                        if(newWords.isEmpty())
                            return@collect

                        bindThreeWords(newWords)
                    }
                }

                launch {
                    viewModel.selectedColorButtonId.collect { id ->
                        binding.colorGroup.check(id)

                        when(id) {
                            R.id.rbBlack -> selectColor(Color.BLACK)
                            R.id.rbBlue -> selectColor(Color.BLUE)
                            R.id.rbGreen -> selectColor(Color.GREEN)
                            R.id.rbOrange -> selectColor(
                                ContextCompat.getColor(this@DrawingActivity, R.color.orange)
                            )
                            R.id.rbRed -> selectColor(Color.RED)
                            R.id.rbYellow -> selectColor(Color.YELLOW)
                            R.id.rbEraser -> {
                                binding.drawingView.setColor(Color.WHITE)
                                binding.drawingView.setThickness(40f)
                            }
                        }
                    }
                }

                launch {
                    viewModel.gameState.collect { gameState ->
                        binding.apply {
                            tvCurWord.text = gameState.word
                            val isUserDrawing = gameState.drawingPlayer == args.username
                            setColorGroupVisibility(isUserDrawing)
                            setMessageInputVisibility(!isUserDrawing)
                            drawingView.isUserDrawing = isUserDrawing
                            drawingView.isEnabled = isUserDrawing
                            ibMic.isVisible = !isUserDrawing
                        }
                    }
                }

                launch {
                    viewModel.players.collect { players ->
                        updatePlayersList(players)
                    }
                }

                launch {
                    viewModel.phaseTime.collect { time ->
                        binding.apply {
                            roundTimerProgressBar.progress = time.toInt()
                            tvRemainingTimeChooseWord.text = (time / 1000L).toString()
                        }
                    }
                }

                launch {
                    viewModel.phase.collect { phaseChange ->
                        bindGamePhase(phaseChange)
                    }
                }

                launch {
                    viewModel.connectionProgressBarVisible.collect { isVisible ->
                        binding.connectionProgressBar.isVisible = isVisible
                    }
                }

                launch {
                    viewModel.chooseWordOverlayVisible.collect { isVisible ->
                        binding.chooseWordOverlay.isVisible = isVisible
                    }
                }
            }
        }
    }

    private fun bindThreeWords(newWords: List<String>) {
        binding.apply {
            btnFirstWord.text = newWords[0]
            btnSecondWord.text = newWords[1]
            btnThirdWord.text = newWords[2]

            btnFirstWord.setOnClickListener {
                viewModel.chooseWord(newWords[0], args.roomName)
                viewModel.setChooseWordOverlayVisibility(false)
            }
            btnSecondWord.setOnClickListener {
                viewModel.chooseWord(newWords[1], args.roomName)
                viewModel.setChooseWordOverlayVisibility(false)
            }
            btnThirdWord.setOnClickListener {
                viewModel.chooseWord(newWords[2], args.roomName)
                viewModel.setChooseWordOverlayVisibility(false)
            }
        }
    }

    private fun bindGamePhase(phaseChange: PhaseChange) {
        when(phaseChange.phase) {
            Room.Phase.WAITING_FOR_PLAYERS -> {
                binding.tvCurWord.text = getString(R.string.waiting_for_players)
                viewModel.cancelTimer()
                viewModel.setConnectionProgressBarVisibility(false)
                binding.roundTimerProgressBar.progress = binding.roundTimerProgressBar.max
            }
            Room.Phase.WAITING_FOR_START -> {
                binding.apply {
                    roundTimerProgressBar.max = phaseChange.time.toInt()
                    tvCurWord.text = getString(R.string.waiting_for_start)
                }
            }
            Room.Phase.NEW_ROUND -> {
                phaseChange.drawingPlayer?.let {
                    binding.tvCurWord.text = getString(R.string.player_is_drawing, phaseChange.drawingPlayer)
                }
                binding.apply {
                    drawingView.isEnabled = false
                    drawingView.setColor(Color.BLACK)
                    drawingView.setThickness(Constants.DEFAULT_PAINT_THICKNESS)
                    roundTimerProgressBar.max = phaseChange.time.toInt()

                    chooseWordOverlay.isVisible = phaseChange.drawingPlayer == args.username
                }
            }
            Room.Phase.GAME_RUNNING -> {
                binding.apply {
                    chooseWordOverlay.isVisible = false
                    roundTimerProgressBar.max = phaseChange.time.toInt()
                }
            }
            Room.Phase.SHOW_WORD -> {
                binding.apply {
                    if(drawingView.isDrawing) {
                        drawingView.finishOffDrawing()
                    }
                    drawingView.apply {
                        isEnabled = false
                        setColor(Color.BLACK)
                        setThickness(Constants.DEFAULT_PAINT_THICKNESS)
                    }
                    roundTimerProgressBar.max = phaseChange.time.toInt()
                }
            }
            else -> Unit
        }
    }

    private fun listenToSocketEvents() = lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.socketEvent.collect { event ->

                when(event) {
                    is SocketEvent.DrawDataEvent -> {

                        val drawData = event.data

                        if(!binding.drawingView.isUserDrawing) {
                            when(drawData.motionEvent) {
                                MotionEvent.ACTION_DOWN -> {
                                    binding.drawingView.startedTouchExternally(drawData)
                                }
                                MotionEvent.ACTION_MOVE -> {
                                    binding.drawingView.movedTouchExternally(drawData)
                                }
                                MotionEvent.ACTION_UP -> {
                                    binding.drawingView.releasedTouchExternally(drawData)
                                }
                            }
                        }
                    }
                    is SocketEvent.RoundDrawInfoEvent -> {
                        binding.drawingView.update(event.data)
                    }
                    is SocketEvent.GameStateEvent -> {
                        binding.drawingView.clear()
                    }
                    is SocketEvent.ChosenWordEvent -> {
                        binding.tvCurWord.text = event.data.chosenWord
                        binding.ibUndo.isEnabled = false
                    }
                    is SocketEvent.ChatMessageEvent -> {
                        addChatObjectToRecyclerView(event.data)
                    }
                    is SocketEvent.AnnouncementEvent -> {
                        addChatObjectToRecyclerView(event.data)
                    }
                    is SocketEvent.UndoEvent -> {
                        binding.drawingView.undo()
                    }
                    is SocketEvent.GameErrorEvent -> {
                        when(event.data.errorType) {
                            GameError.ERROR_ROOM_NOT_FOUND -> onBackPressedDispatcher.onBackPressed()
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun listenToConnectionEvents() = lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {

            viewModel.connectionEvent.collect { event ->
                when(event) {
                    is WebSocket.Event.OnConnectionOpened<*> -> {
                        viewModel.sendBaseModel(
                            JoinRoomHandshake(
                                username = args.username,
                                roomName = args.roomName,
                                clientId = clientId
                            )
                        )
                        viewModel.setConnectionProgressBarVisibility(false)
                    }
                    is WebSocket.Event.OnConnectionFailed -> {
                        viewModel.setConnectionProgressBarVisibility(false)
                        Snackbar.make(
                            binding.root,
                            R.string.error_connection_failed,
                            Snackbar.LENGTH_LONG
                        ).show()
                        event.throwable.printStackTrace()
                    }
                    is WebSocket.Event.OnConnectionClosed -> {
                        viewModel.setConnectionProgressBarVisibility(false)
                    }
                    else -> Unit
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        /**
         * save the scroll state on config. changes
         */
        binding.rvChat.layoutManager?.onSaveInstanceState()
    }

    private fun updatePlayersList(players: List<PlayerData>) {
        updatePlayersJob?.cancel()
        updatePlayersJob = lifecycleScope.launch {
            playerAdapter.updateDataset(players)
        }
    }

    private fun updateChatMessageList(chat: List<BaseModel>) {
        updateChatJob?.cancel()
        updateChatJob = lifecycleScope.launch {
            chatMessageAdapter.updateDataset(chat)
        }
    }

    /**
     * called when single chat object from socket..
     */
    private suspend fun addChatObjectToRecyclerView(chatObject: BaseModel) {
        val canScrollDown = binding.rvChat.canScrollVertically(1)
        updateChatMessageList(chatMessageAdapter.chatObjects + chatObject)
        updateChatJob?.join()

        /**
         * scroll to down when user reached last item and prevent when user in middle of the list..
         */
        if(!canScrollDown) {
            binding.rvChat.scrollToPosition(chatMessageAdapter.chatObjects.size - 1)
        }
    }

    private fun setupRecyclerView() = binding.rvChat.apply {
        chatMessageAdapter = ChatMessageAdapter(args.username)
        adapter = chatMessageAdapter
        layoutManager = LinearLayoutManager(this@DrawingActivity)

        /**
         * restore the scroll state when config. changes
         */
        chatMessageAdapter.stateRestorationPolicy =
            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}