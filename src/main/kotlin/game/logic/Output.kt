package game.logic

import game.domain.Game
import game.domain.Player
import game.domain.Round

interface Output {
    fun showGameState(game: Game, roundNumber: Int)
    fun showGameResult(game: Game)
    fun showRoundResult(round: Round)
    fun showError(message: String)
}
