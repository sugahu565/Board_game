package game.domain

import game.domain.cards.SearchCard

enum class RoundStatus { IN_PROGRESS, MONSTER_WON, HUNTERS_WON }

class Round(
    private val players: List<Player>,
    val deck: Deck
) {
    private var currentPlayerIndex = 0
    private var startPlayerIndex = 0
    private var currentTurn = 1
    private val history = mutableListOf<Move>()

    var status: RoundStatus = RoundStatus.IN_PROGRESS
        private set

    init {
        players.forEach { player -> repeat(4) { player.addCard(deck.nextCard()) } }
        players.indexOfFirst { SearchCard in it.getCards() }.takeIf { it >= 0 }?.let {
            currentPlayerIndex = it
            startPlayerIndex = it
        }
    }

    fun checkMove(move: Move): String? {
        if (status != RoundStatus.IN_PROGRESS) return "Раунд уже закончился"
        if (move.initiator != players[currentPlayerIndex]) return "Игрок ходит не в свой ход"
        if (move.playerCard !in move.initiator.getCards()) return "Этой карты нет в руке игрока"
        if (SearchCard in move.initiator.getCards() && move.playerCard != SearchCard) {
            return "Вы должны сыграть карту 'Розыск'"
        }
        return null
    }

    fun recordMove(move: Move, result: MoveResult) {
        require(result is MoveResult.Applied) { "Нельзя записать отклонённый ход" }
        check(status == RoundStatus.IN_PROGRESS && move.initiator == players[currentPlayerIndex]) {
            "Нельзя записать ход в текущем состоянии раунда"
        }
        when (result.roundStatus) {
            RoundStatus.MONSTER_WON -> finishWithMonsterWin()
            RoundStatus.HUNTERS_WON -> finishWithHuntersWin()
            RoundStatus.IN_PROGRESS -> Unit
        }
        history.add(move)
        if (status == RoundStatus.IN_PROGRESS) nextPlayer()
    }

    private fun nextPlayer() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size
        if (currentPlayerIndex == startPlayerIndex) currentTurn++
    }

    fun getCurrentTurn(): Int = currentTurn
    fun getCurrentPlayer(): Player = players[currentPlayerIndex]
    fun getMoveLog(): List<MoveLog> = history.map { it.toLog() }
    fun getPlayers(): List<Player> = players.toList()
    fun countPlayedCards(predicate: (Card) -> Boolean): Int = history.count { predicate(it.playerCard) }
    fun finishWithMonsterWin() { status = RoundStatus.MONSTER_WON }
    fun finishWithHuntersWin() { status = RoundStatus.HUNTERS_WON }
}
