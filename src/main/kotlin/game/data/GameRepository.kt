package game.data

import game.domain.GameResult
import game.domain.MoveLog

interface GameRepository {
    fun registerPlayer(name: String)
    fun writeGame(result: GameResult): Int
    fun getHistory(): List<StoredGame>
    fun getStats(): List<PlayerStatistics>
    fun deleteGame(gameId: Int): Boolean
}

data class StoredGame(
    val id: Int,
    val playedAt: Long,
    val players: List<StoredPlayerResult>,
    val history: List<List<MoveLog>>
)

data class StoredPlayerResult(
    val uid: Int,
    val name: String,
    val score: Int,
    val winner: Boolean
)

data class PlayerStatistics(
    val name: String,
    val gamesPlayed: Int,
    val wins: Int,
    val totalScore: Int
) {
    val averageScore: Double
        get() = if (gamesPlayed == 0) 0.0 else totalScore.toDouble() / gamesPlayed
}
