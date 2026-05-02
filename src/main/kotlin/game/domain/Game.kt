package game.domain

import game.domain.cards.Friend
import game.domain.cards.Monster
import game.logic.Output
import kotlin.collections.forEach

enum class GameStatus { STAGED, IN_PROGRESS, FINISHED }

class Game(private val players: List<Player>, private val output: Output) {
    private var round: Round? = null
    private var roundNumber: Int = 0
    var status: GameStatus = GameStatus.STAGED
        private set

    private val moveHistory: MutableList<List<MoveLog>> = mutableListOf()
    private val scores: MutableMap<Player, Int> = players.associateWith { 0 }.toMutableMap()

    companion object {
        const val TOTAL_ROUNDS = 5
    }

    fun startNextRound(deck: Deck) {
        if (roundNumber >= TOTAL_ROUNDS) {
            output.showError("Все раунды уже сыграны")
            return
        }
        roundNumber++
        round = Round(players, deck, output)
        status = GameStatus.IN_PROGRESS
    }

    fun processGameMove(move: Move) {
        val currentRound = round ?: run {
            output.showError("Раунд ещё не начался")
            return
        }
        currentRound.processMove(move)

        if (currentRound.status != RoundStatus.IN_PROGRESS) {
            moveHistory.add(currentRound.getHistory())
            updateScores(currentRound)
            if (roundNumber >= TOTAL_ROUNDS) {
                status = GameStatus.FINISHED
                output.showGameResult(this)
            } else {
                output.showGameState(this, roundNumber)
            }
        }
    }

    private fun updateScores(round: Round) {
        when (round.status) {
            RoundStatus.MONSTER_WON -> {
                round.getPlayers().forEach { player ->
                    val maxRoleCard = player.getPlayed()
                        .filter { it is Monster || it is Friend }
                        .maxByOrNull { it.winPoints }

                    if (maxRoleCard != null) {
                        scores[player] = (scores[player] ?: 0) + maxRoleCard.winPoints
                    }
                }
            }
            RoundStatus.HUNTERS_WON -> {
                val heroPlayer = moveHistory.lastOrNull()?.lastOrNull()?.initiatorUID
                round.getPlayers().forEach { player ->
                    val isMonster = player.getPlayed().any { it is Monster }
                    val isFriend = player.getPlayed().any { it is Friend }
                    if (!(isMonster || isFriend)) {
                        if (heroPlayer == player.UID)
                            scores[player] = (scores[player] ?: 0) + 2
                        else
                            scores[player] = (scores[player] ?: 0) + 1
                    }
                }
            }
            else -> {}
        }
    }

    fun getScores(): Map<Player, Int> = scores.toMap()

    fun getResult(): GameResult {
        if (status != GameStatus.FINISHED) throw IllegalStateException("Игра ещё не завершена")

        val maxScore = scores.values.maxOrNull() ?: 0
        val winners = scores.filter { it.value == maxScore }.keys.toList()

        return GameResult(
            winners = winners,
            history = moveHistory.toList()
        )
    }
}
