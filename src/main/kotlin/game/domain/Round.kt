package game.domain

import game.logic.Output
import game.domain.cards.SearchCard

enum class RoundStatus { IN_PROGRESS, MONSTER_WON, HUNTERS_WON }

class Round(
    private val players: List<Player>,
    val deck: Deck, // публичный - карты могут заставлять брать из колоды
    private val output: Output
) {
    private var currentPlayerIndex: Int = 0
    private var startPlayerIndex: Int = 0
    private var currentTurn: Int = 1
    var history: MutableList<Move> = mutableListOf()
        private set // костыль для валидации некоторых карт
    var status: RoundStatus = RoundStatus.IN_PROGRESS
        private set

    init {
        players.forEach { player ->
            player.addCard(deck.nextCard())
        }
        val searchCardHolderIndex = players.indexOfFirst { player ->
            player.getCards().any { it is SearchCard }
        }
        if (searchCardHolderIndex != -1) {
            currentPlayerIndex = searchCardHolderIndex
            startPlayerIndex = searchCardHolderIndex
        }
    }

    fun processMove(move: Move) {
        if (status != RoundStatus.IN_PROGRESS) {
            output.showError("Раунд уже закончился")
            return
        }
        if (move.initiator != players[currentPlayerIndex]) {
            output.showError("Игрок ходит не в свой ход")
            return
        }
        val error: String? = move.playerCard.validate(move, this)
        if (error != null) {
            output.showError(error)
            return
        }
        move.playerCard.play(move, this)
        history.add(move)
        nextPlayer()
    }

    private fun nextPlayer() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size
        if (currentPlayerIndex == startPlayerIndex)
            currentTurn++
    }

    fun getCurrentTurn(): Int = currentTurn

    fun getCurrentPlayer(): Player = players[currentPlayerIndex]

    fun getHistory(): List<MoveLog> = history.map { it.toLog() }

    fun getPlayers(): List<Player> = players.toList()

    fun finishWithMonsterWin() {
        status = RoundStatus.MONSTER_WON
    }

    fun finishWithHuntersWin() {
        status = RoundStatus.HUNTERS_WON
    }
}
