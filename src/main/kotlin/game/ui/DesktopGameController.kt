package game.ui

import game.data.GameRepository
import game.data.PlayerStatistics
import game.data.StoredGame
import game.domain.*

class DesktopGameController(
    private val repository: GameRepository,
    private val gameFactory: (List<Player>) -> Game = { Game(it) }
) {
    var players: List<PlayerStatistics> = emptyList()
        private set
    var history: List<StoredGame> = emptyList()
        private set
    val selectedNames = linkedSetOf<String>()
    var game: Game? = null
        private set
    var handVisible = false
        private set
    var savedGameId: Int? = null
        private set

    init { refresh() }

    fun refresh() {
        players = repository.getStats()
        history = repository.getHistory()
    }

    fun addPlayer(name: String) {
        check(game == null) { "Состав нельзя менять во время игры" }
        require(selectedNames.size < 8) { "В партии может быть не больше 8 игроков" }

        val clean = name.trim()
        require(clean.isNotEmpty()) { "Имя не может быть пустым" }
        require(selectedNames.none { it.equals(clean, ignoreCase = true) }) { "Игрок уже добавлен в партию" }

        val existing = players.firstOrNull { it.name == clean }
            ?: players.firstOrNull { it.name.equals(clean, ignoreCase = true) }

        if (existing == null) {
            repository.registerPlayer(clean)
            refresh()
        }

        selectedNames.add(existing?.name ?: clean)
    }

    fun removePlayer(name: String) {
        check(game == null) { "Состав нельзя менять во время игры" }
        selectedNames.remove(name)
    }

    fun startGame() {
        check(game == null) { "Партия уже идёт" }
        val names = selectedNames.toList()
        require(names.size in 3..8) { "Выберите от 3 до 8 игроков" }

        game = gameFactory(names.mapIndexed { index, name -> Player(index + 1, name) })
        handVisible = false
        savedGameId = null
    }

    fun revealHand() {
        val current = game ?: error("Сначала начните игру")
        check(current.status == GameStatus.IN_PROGRESS)
        handVisible = true
    }

    fun visibleCards(): List<Card> =
        if (handVisible) game?.getCurrentPlayer()?.getCards().orEmpty() else emptyList()

    fun hideHand() { handVisible = false }

    fun submitMove(cardIndex: Int, targetUid: Int?, details: MoveDetails): GameMoveResult {
        val current = game ?: error("Сначала начните игру")
        check(handVisible) { "Сначала откройте руку текущего игрока" }
        val initiator = current.getCurrentPlayer()
        val card = initiator.getCards().getOrNull(cardIndex) ?: error("Карта не выбрана")
        val target = current.getAllPlayers().firstOrNull { it.UID == targetUid }
        val result = current.processGameMove(Move(initiator, target, card, details))
        if (result !is GameMoveResult.Rejected) {
            handVisible = false
            if (current.status == GameStatus.FINISHED) {
                savedGameId = repository.writeGame(current.getResult())
                refresh()
            }
        }
        return result
    }

    fun returnToLobby() {
        check(game?.status == GameStatus.FINISHED) { "Партия ещё не закончилась" }
        game = null
        handVisible = false
        selectedNames.clear()
        refresh()
    }
}
