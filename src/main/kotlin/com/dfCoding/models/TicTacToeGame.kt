package com.dfCoding.models

import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

class TicTacToeGame {

    private val state = MutableStateFlow(GameState())

    private val playerSockets = ConcurrentHashMap<Char, WebSocketSession>()

    private val gameScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var delayGameJob: Job? = null

    init {
        state.onEach(::broadcast).launchIn(gameScope) // when ever state changes call broadcast function with the new state
    }

    fun connectPlayer(session: WebSocketSession): Char? {
        val isPlayerX = state.value.connectedPlayers.any { it == 'X'}
        val player = if(isPlayerX) 'O' else 'X'

        state.update {
            if(state.value.connectedPlayers.contains(player)) { // don't allow more than 2 players
                return null
            }
            if(!playerSockets.containsKey(player)) {
                playerSockets[player] = session
            }

            it.copy(
                connectedPlayers = it.connectedPlayers + player
            )
        }
        return player
    }

    fun disconnectPlayer(player: Char) {
        playerSockets.remove(player)
        state.update {
            it.copy(
                connectedPlayers = it.connectedPlayers - player
            )
        }
    }

    suspend fun broadcast(state: GameState) {
        playerSockets.values.forEach { socket ->
            socket.send(
                Json.encodeToString(state)
            )
        }
    }

    fun finishTurn(player: Char, x: Int, y: Int) {
        if(state.value.field[y][x] != null || state.value.winningPlayer != null) {
            return // if there is already x or o in that spot return or if there is already a winner return
        }

        if (state.value.playerAtTurn != player) {
            return // if its not this players turn dont let him finish the turn (its not his turn)
        }

        val currentPlayer = state.value.playerAtTurn
        state.update {
            val newField = it.field.also { field ->
                field[y][x] = currentPlayer
            }
            val isBoardFull = newField.all { it.all { it != null } }
            if (isBoardFull) {
                startNewRoundDelayed() // start new game after some time when full
            }
            it.copy(
                playerAtTurn = if(currentPlayer == 'X') 'O' else 'X',
                field = newField,
                isBoardFull = isBoardFull,
                winningPlayer = getWinningPlayer()?.also {
                    startNewRoundDelayed()
                }
            )
        }
    }

    private fun getWinningPlayer(): Char? {
        val field = state.value.field
        return if (field[0][0] != null && field[0][0] == field[0][1] && field[0][1] == field[0][2]) {
            field[0][0]
        } else if (field[1][0] != null && field[1][0] == field[1][1] && field[1][1] == field[1][2]) {
            field[1][0]
        } else if (field[2][0] != null && field[2][0] == field[2][1] && field[2][1] == field[2][2]) {
            field[2][0]
        } else if (field[0][0] != null && field[0][0] == field[1][0] && field[1][0] == field[2][0]) {
            field[0][0]
        } else if (field[0][1] != null && field[0][1] == field[1][1] && field[1][1] == field[2][1]) {
            field[0][1]
        } else if (field[0][2] != null && field[0][2] == field[1][2] && field[1][2] == field[2][2]) {
            field[0][2]
        } else if (field[0][0] != null && field[0][0] == field[1][1] && field[1][1] == field[2][2]) {
            field[0][0]
        } else if (field[0][2] != null && field[0][2] == field[1][1] && field[1][1] == field[2][0]) {
            field[0][2]
        } else null
    }

    private fun startNewRoundDelayed() {
        delayGameJob?.cancel()
        delayGameJob = gameScope.launch {
            delay(5000L)
            state.update {
                it.copy(
                    playerAtTurn = 'X',
                    field = GameState.emptyField(),
                    winningPlayer = null,
                    isBoardFull = false,
                )
            }
        }
    }
}