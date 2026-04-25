package game.domain

data class GameResult(
    val winnerName: String,
    val winnerUID: Int,
    val history: List<List<MoveLog>>
)