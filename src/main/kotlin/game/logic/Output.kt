package game.logic

import game.data.PlayerStatistics
import game.data.StoredGame
import game.domain.Game
import game.domain.RoundStatus
import game.domain.MoveEvent

interface Output {
    fun showGameState(game: Game, roundNumber: Int)
    fun showGameResult(game: Game)
    fun showRoundResult(status: RoundStatus)
    fun showMoveEvent(event: MoveEvent)
    fun showError(message: String)
    fun showGameSaved(gameId: Int)
    fun showHistory(history: List<StoredGame>)
    fun showStats(statistics: List<PlayerStatistics>)
}
