package game.ui

import game.logic.Output
import game.logic.Input
import game.domain.Game
import game.domain.Move
import game.domain.Round
import game.domain.Player

class Console : Output, Input {
    override fun showGameState(game: Game, roundNumber: Int) {
        println("Раунд ${roundNumber} из ${Game.TOTAL_ROUNDS}")
    }

    override fun showGameResult(game: Game) {
        val result = game.getResult()
        println("Игра окончена! Победили: ${result.winners.joinToString { it.name }}")
    }

    override fun showRoundResult(round: Round) {
        println("Раунд завершён. Результат: ${round.status}")
    }

    override fun showError(message: String) {
        println("ОШИБКА: $message")
    }

    override fun inputPlayers(): List<Player> {
        // TODO: ДОПИЛИТЬ
        return listOf()
    }

    override fun inputMove(): Move {
        TODO("Not yet implemented")
    }
}
