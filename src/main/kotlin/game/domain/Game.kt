package game.domain

enum class GameStatus {STAGED, FINISHED}

class Game(private val players: List<Player>) {
    var round: Round? = null
    var status: GameStatus = GameStatus.STAGED
    private var scores: Map<Player, Int> = players.associateWith { 0 }

    fun processGameMove(move: Move) {
        // Разобраться с тем, где строка превращается в Move (мб как раз тут, либо в Round)

    }

    fun getResult(): GameResult {

    }
}