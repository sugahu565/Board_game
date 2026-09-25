package game.logic

import game.data.GameRepository
import game.domain.*

class GameAdmin(
    private val gameRepository: GameRepository,
    private val input: Input,
    private val output: Output,
    private val gameFactory: (List<Player>) -> Game = { Game(it) }
) {
    fun runGame() {
        val game = gameFactory(input.inputPlayers())
        output.showGameState(game, game.getRoundNumber())

        while (game.status != GameStatus.FINISHED) {
            when (val result = game.processGameMove(input.inputMove(game))) {
                is GameMoveResult.Rejected -> output.showError(result.message)
                is GameMoveResult.MoveApplied -> result.event?.let(output::showMoveEvent)
                is GameMoveResult.RoundFinished -> {
                    result.event?.let(output::showMoveEvent)
                    output.showRoundResult(result.roundStatus)
                    if (result.gameFinished) output.showGameResult(game)
                    else output.showGameState(game, game.getRoundNumber())
                }
            }
        }

        val gameId = gameRepository.writeGame(game.getResult())
        output.showGameSaved(gameId)
        output.showHistory(gameRepository.getHistory())
        output.showStats(gameRepository.getStats())
    }
}
