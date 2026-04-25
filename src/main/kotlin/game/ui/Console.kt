package game.ui

import game.logic.Output
import game.domain.Game
import game.domain.Round

class ConsoleOutput : Output {
    override fun showGameState(game: Game) {

    }
    override fun showGameResult(game: Game) {

    }
    override fun showRoundResult(round: Round) {

    }

    override fun showError(message: String) {
        println("ОШИБКА: $message")
    }
}
