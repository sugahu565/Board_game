package game.logic
import game.domain.*

interface Output {
    fun showGameState(game: Game)
    fun showGameResult(game: Game)
    fun showRoundResult(round: Round)
    fun showError(message: String)
}
