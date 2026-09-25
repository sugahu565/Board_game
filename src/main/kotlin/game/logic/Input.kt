package game.logic

import game.domain.Player
import game.domain.Move
import game.domain.Game

interface Input {
    fun inputPlayers(): List<Player>
    fun inputMove(game: Game): Move
}