package game.ui

import game.data.PlayerStatistics
import game.data.StoredGame
import game.logic.Output
import game.logic.Input
import game.domain.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class Console : Output, Input {
    override fun showGameState(game: Game, roundNumber: Int) {
        println("Раунд ${roundNumber} из ${Game.TOTAL_ROUNDS}")
    }

    override fun showGameResult(game: Game) {
        val result = game.getResult()
        println("Игра окончена! Победили: ${result.winners.joinToString { it.name }}")
    }

    override fun showRoundResult(status: RoundStatus) {
        println("Раунд завершён. Результат: $status")
    }

    override fun showMoveEvent(event: MoveEvent) {
        when (event) {
            is MoveEvent.CardsRevealed -> println(
                "Карты игрока ${event.player.name}: " + event.cards.joinToString { it.title }
            )
        }
    }

    override fun showError(message: String) {
        println("ОШИБКА: $message")
    }

    override fun showGameSaved(gameId: Int) {
        println("Партия #$gameId сохранена в базу данных.")
    }

    override fun showHistory(history: List<StoredGame>) {
        println("\nПоследние партии:")
        history.take(5).forEach { game ->
            val date = Instant.ofEpochMilli(game.playedAt)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
            val winners = game.players.filter { it.winner }.joinToString { it.name }
            println("#${game.id} | $date | победители: $winners")
        }
    }

    override fun showStats(statistics: List<PlayerStatistics>) {
        println("\nСтатистика игроков:")
        statistics.forEach { stat ->
            println(
                "${stat.name}: игр ${stat.gamesPlayed}, побед ${stat.wins}, " +
                    "очков ${stat.totalScore}, среднее %.2f".format(stat.averageScore)
            )
        }
    }

    override fun inputPlayers(): List<Player> {
        print("Введите количество игроков (от 3 до 8): ")
        val count = readln().toIntOrNull()?.coerceIn(3, 8) ?: 3

        val players = mutableListOf<Player>()
        val usedNames = mutableSetOf<String>()
        for (i in 1..count) {
            var name: String
            do {
                print("Введите имя игрока $i: ")
                name = readln().trim().takeIf { it.isNotEmpty() } ?: "Игрок $i"
                if (name.lowercase() in usedNames) println("Имена игроков должны быть разными.")
            } while (name.lowercase() in usedNames)
            usedNames.add(name.lowercase())
            players.add(Player(UID = i, name = name))
        }
        return players
    }

    override fun inputMove(game: Game): Move {
        val currentPlayer = game.getCurrentPlayer()
        val allPlayers = game.getAllPlayers()

        println("\n[ Ходит игрок: ${currentPlayer.name} ]")

        val hand = currentPlayer.getCards()
        println("Ваши карты:")
        hand.forEachIndexed { index, card ->
            println("${index + 1}. ${card.title} - ${card.description}")
        }
        print("Выберите номер карты: ")
        val cardIndex = (readln().toIntOrNull() ?: 1) - 1
        val playedCard = hand.getOrNull(cardIndex) ?: hand.first()

        var targetPlayer: Player? = null
        if (playedCard.requiresTarget) {
            println("Выберите цель:")
            allPlayers.forEachIndexed { index, player ->
                println("${index + 1}. ${player.name}")
            }
            print("Номер игрока-цели: ")
            val targetIndex = (readln().toIntOrNull() ?: 1) - 1
            targetPlayer = allPlayers.getOrNull(targetIndex) ?: allPlayers.first()
        }

        val details = collectMoveDetails(playedCard, currentPlayer, targetPlayer, allPlayers)

        return Move(
            initiator = currentPlayer,
            target = targetPlayer,
            playerCard = playedCard,
            details = details
        )
    }

    private fun collectMoveDetails(
        card: Card, initiator: Player, target: Player?, allPlayers: List<Player>
    ): MoveDetails = when (card.effect) {
        CardEffect.THEFT -> MoveDetails.Theft(
            selectCards(initiator, "Выберите карту, которую отдадите:", card).firstOrNull()
        )
        CardEffect.TRADE -> MoveDetails.Trade(
            selectCards(initiator, "Выберите 1 или 2 карты для обмена:", card),
            target?.let { selectCards(it, "${it.name}, выберите карты для обмена:") } ?: emptyList()
        )
        CardEffect.FOG -> MoveDetails.Fog(allPlayers.filter { it != initiator }.mapNotNull { player ->
            selectCards(player, "${player.name}, выберите карту для Тумана:").firstOrNull()?.let { CardChoice(player, it) }
        })
        CardEffect.GOSSIP -> MoveDetails.Gossip(allPlayers.mapNotNull { player ->
            selectCards(player, "${player.name}, выберите карту для Сплетен:", if (player == initiator) card else null)
                .firstOrNull()?.let { CardChoice(player, it) }
        })
        CardEffect.HOUND -> MoveDetails.Hound(
            target?.let { selectCards(it, "${it.name}, выберите карту для сброса:").firstOrNull() }
        )
        else -> MoveDetails.None
    }

    private fun selectCards(player: Player, promptMsg: String, playedCard: Card? = null): List<Card> {
        val availableCards = player.getCards().toMutableList()

        if (playedCard != null) {
            availableCards.remove(playedCard)
        }

        if (availableCards.isEmpty()) return emptyList()

        println("\n$promptMsg")
        availableCards.forEachIndexed { index, c -> println("${index + 1}. ${c.title}") }
        print("Ввод: ")

        val input = readln()
        val indices = input.split(Regex("[\\s,]+")).mapNotNull { it.toIntOrNull() }

        return indices.mapNotNull { idx -> availableCards.getOrNull(idx - 1) }
    }
}
