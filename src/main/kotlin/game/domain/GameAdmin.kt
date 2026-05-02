package game.domain

import game.data.GameRepository
import game.logic.Output
import game.logic.Input

class GameAdmin(
    private val gameRepository: GameRepository,
    private val input: Input,
    private val output: Output
) {
    private var currentGame: Game? = null

    fun runGame() {
        val players: List<Player> = input.inputPlayers()
        currentGame = Game(players, output)
        while (currentGame?.status != GameStatus.FINISHED) {
            val move = input.inputMove()
            currentGame?.processGameMove(move)
        }
        val result: GameResult = currentGame!!.getResult()
        gameRepository.writeGame(result)
    }
}