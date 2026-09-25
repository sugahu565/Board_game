package game.domain

enum class GameStatus { STAGED, IN_PROGRESS, FINISHED }

sealed interface GameMoveResult {
    data class Rejected(val message: String) : GameMoveResult
    data class MoveApplied(val event: MoveEvent?) : GameMoveResult
    data class RoundFinished(
        val roundStatus: RoundStatus,
        val gameFinished: Boolean,
        val event: MoveEvent?
    ) : GameMoveResult
}

class Game(
    private val players: List<Player>,
    private val effectHandler: CardEffectHandler = DefaultCardEffectHandler()
) {
    private var round: Round? = null
    private var roundNumber = 0
    private val moveHistory = mutableListOf<List<MoveLog>>()
    private val scores = players.associateWith { 0 }.toMutableMap()

    var status: GameStatus = GameStatus.STAGED
        private set

    companion object {
        const val TOTAL_ROUNDS = 1
    }

    init {
        require(players.size in 3..8) { "В игре должно быть от 3 до 8 игроков" }
        startNextRound()
    }

    private fun startNextRound() {
        if (roundNumber >= TOTAL_ROUNDS) return
            roundNumber++
        players.forEach { it.resetForRound() }
        round = Round(players, Deck(players.size))
        status = GameStatus.IN_PROGRESS
    }

    fun processGameMove(move: Move): GameMoveResult {
        val currentRound = round ?: return GameMoveResult.Rejected("Раунд ещё не начался")
        currentRound.checkMove(move)?.let { return GameMoveResult.Rejected(it) }
        effectHandler.validate(move, currentRound)?.let { return GameMoveResult.Rejected(it) }
        val result = when (val effect = effectHandler.apply(move, currentRound)) {
            is MoveResult.Rejected -> return GameMoveResult.Rejected(effect.message)
            is MoveResult.Applied -> effect
        }
        currentRound.recordMove(move, result)

        if (currentRound.status == RoundStatus.IN_PROGRESS)
            return GameMoveResult.MoveApplied(result.event)

        moveHistory.add(currentRound.getMoveLog())
        updateScores(currentRound)
        if (roundNumber >= TOTAL_ROUNDS) status = GameStatus.FINISHED else startNextRound()
        return GameMoveResult.RoundFinished(currentRound.status, status == GameStatus.FINISHED, result.event)
    }

    private fun updateScores(round: Round) {
        when (round.status) {
            RoundStatus.MONSTER_WON -> round.getPlayers().forEach { player ->
                player.getPlayed()
                    .filter { it.role == CardRole.MONSTER || it.role == CardRole.FRIEND }
                    .maxByOrNull { it.winPoints }
                    ?.let { scores[player] = scores.getValue(player) + it.winPoints }
            }
            RoundStatus.HUNTERS_WON -> {
                val heroUID = moveHistory.lastOrNull()?.lastOrNull()?.initiatorUID
                round.getPlayers().forEach { player ->
                    val playsMonsterSide = player.getPlayed().any {
                        it.role == CardRole.MONSTER || it.role == CardRole.FRIEND
                    }
                    if (!playsMonsterSide) scores[player] = scores.getValue(player) + if (player.UID == heroUID) 2 else 1
                }
            }
            RoundStatus.IN_PROGRESS -> Unit
        }
    }

    fun getScores(): Map<Player, Int> = scores.toMap()

    fun getResult(): GameResult {
        check(status == GameStatus.FINISHED) { "Игра ещё не завершена" }
        val maxScore = scores.values.maxOrNull() ?: 0
        return GameResult(
            winners = scores.filterValues { it == maxScore }.keys.toList(),
            history = moveHistory.toList(),
            scores = scores.toMap()
        )
    }

    fun getCurrentPlayer(): Player = round?.getCurrentPlayer() ?: error("Раунд не запущен")
    fun getAllPlayers(): List<Player> = players.toList()
    fun getRoundNumber(): Int = roundNumber
    fun getCurrentTurn(): Int = round?.getCurrentTurn() ?: error("Раунд не запущен")
}
