package game.logic

import game.domain.Player
import game.domain.Move
import game.domain.Card

interface Input {
    fun inputPlayers(): List<Player>
    fun inputMove(): Move
}