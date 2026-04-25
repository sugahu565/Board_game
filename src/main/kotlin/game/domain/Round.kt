package game.domain
import game.logic.Output

enum class RoundStatus {IN_PROGRESS, FINISHED}

class Round(
    val players: List<Player>,
    private val output: Output
) {
    private val currentPlayerIndex: Int = players.size
    private val currentRound: Int = 1
    private val deck: Deck = Deck(currentPlayerIndex)
    private val history: MutableList<Move> = mutableListOf<Move>()
    var status: RoundStatus = RoundStatus.IN_PROGRESS

    fun processMove(move: Move) {
        if (status == RoundStatus.FINISHED) {
            output.showError("Раунд уже закончился")
            return
        }
        if (move.initiator != players[currentPlayerIndex]) {
            output.showError("Игрок ходит не в свой ход")
            return
        }
        val message: String? = move.playerCard.validate(move, this)
        if (message != null) {
            output.showError(message)
            return
        }
    }

    fun getCurrentRound(): Int {
        return currentRound
    }
}
