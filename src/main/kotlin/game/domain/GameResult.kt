package game.domain

data class GameResult(
    val winners: List<Player>,
    val history: List<List<MoveLog>>
)
