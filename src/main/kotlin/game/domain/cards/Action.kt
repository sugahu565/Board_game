package game.domain.cards

import game.domain.Card
import game.domain.Move
import game.domain.Round
import game.domain.Player

abstract class Action : Card()

object SearchCard : Card() {
    override val title: String = "Розыск"
    override val description: String = "Вы начинаете раунд"

    override fun validate(move: Move, round: Round): String? = null

    override fun play(move: Move, round: Round) {
        move.initiator.playCard(this)
    }
}

object Villager : Action() {
    override val title: String = "Мирный житель"
    override val description: String = "Без эффектов"
    override fun validate(move: Move, round: Round): String? {
        return null
    }
    override fun play(move: Move, round: Round) {
        move.initiator.playCard(this)
    }
}

object RumorsCard : Action() {
    override val title: String = "Слухи"
    override val description: String = "Каждый игрок получает случайную карту от соседа справа. Не работает на 4 ходе"

    override fun validate(move: Move, round: Round): String? = null

    override fun play(move: Move, round: Round) {
        move.initiator.playCard(this)

        if (round.getCurrentTurn() >= 4) return // сброс без эффекта

        val players = round.getPlayers()

        val cardsToPass = players.associateWith { player ->
            val randomCard = player.getCards().randomOrNull()
            if (randomCard != null) player.removeCard(randomCard)
            randomCard
        }

        players.forEachIndexed { index, player ->
            val cardFromThisPlayer = cardsToPass[player]
            if (cardFromThisPlayer != null) {
                val rightNeighborIndex = (index + 1) % players.size
                val rightNeighbor = players[rightNeighborIndex]
                rightNeighbor.addCard(cardFromThisPlayer)
            }
        }
    }
}

object TheftCard : Action() {
    override val title: String = "Кража"
    override val description: String = "Заберите случайную карту у цели, затем отдайте ей одну свою. Не работает на 4 ходе."

    override fun validate(move: Move, round: Round): String? {
        if (round.getCurrentTurn() >= 4) return null

        if (move.target == null) return "Выберите игрока для кражи"
        if (move.target == move.initiator) return "Нельзя использовать на себя"

        val cardToGive = move.additionalData?.get(move.initiator)
        if (cardToGive == null && move.initiator.getCards().isNotEmpty()) {
            return "Выберите карту со своей руки, чтобы отдать её после кражи"
        }

        return null
    }

    override fun play(move: Move, round: Round) {
        move.initiator.playCard(this)

        if (round.getCurrentTurn() >= 4) return

        val target = move.target!!

        val stolenCard = target.getCards().randomOrNull()
        if (stolenCard != null) {
            target.removeCard(stolenCard)
            move.initiator.addCard(stolenCard)
        }

        val cardToGive = move.additionalData?.get(move.initiator)?.firstOrNull()
        if (cardToGive != null) {
            move.initiator.removeCard(cardToGive)
            target.addCard(cardToGive)
        }
    }
}

object InvestigationCard : Card() {
    override val title: String = "Расследование"
    override val description: String = "Посмотрите карты выбранного игрока."

    override fun validate(move: Move, round: Round): String? {
        if (move.target == null) return "Необходимо выбрать игрока"
        if (move.target == move.initiator) return "Нельзя использовать карту на себе"
        return null
    }

    override fun play(move: Move, round: Round) {
        move.initiator.playCard(this)

        val targetCards = move.target!!.getCards()
        //TODO(как - то вывести)
    }
}

object GossipCard : Card() {
    override val title: String = "Сплетни"
    override val description: String = "Каждый игрок получает выбранную карту от соседа слева. Не работает на 4 ходе"

    override fun validate(move: Move, round: Round): String? {
        if (round.getCurrentTurn() >= 4) return null // на 4 ходе ничего не проверяем

        val players = round.getPlayers()
        for (player in players) { // проверяем, что каждый игрок выбрал карту
            if (move.additionalData?.get(player)?.firstOrNull() == null) {
                return "Не все игроки выбрали карты для передачи"
            }
        }
        return null
    }

    override fun play(move: Move, round: Round) {
        move.initiator.playCard(this)
        if (round.getCurrentTurn() >= 4) return // 4 ход

        val players = round.getPlayers()

        val cardsToPass = mutableMapOf<Player, Card>()
        players.forEach { player -> // делаю мапу по игрокам и выбранным картам + удаляю карты с рук
            val cardToGive = move.additionalData?.get(player)?.firstOrNull()
            if (cardToGive != null) {
                player.removeCard(cardToGive)
                cardsToPass[player] = cardToGive
            }
        }

        players.forEachIndexed { index, player -> // раздача соседям. i игрок отдаёт карту i - 1
            val cardFromThisPlayer = cardsToPass[player]
            if (cardFromThisPlayer != null) {
                val leftNeighborIndex = (index - 1 + players.size) % players.size
                players[leftNeighborIndex].addCard(cardFromThisPlayer)
            }
        }
    }
}

object FogCard : Card() {
    override val title: String = "Туман"
    override val description: String = "Все остальные сдают по 1 карте. Карты перемешиваются и раздаются обратно. Не работает на 4 ходе"

    override fun validate(move: Move, round: Round): String? {
        if (round.getCurrentTurn() >= 4) return null

        val otherPlayers = round.getPlayers().filter { it != move.initiator }
        for (player in otherPlayers) {
            if (player.getCards().isNotEmpty() && move.additionalData?.get(player)?.firstOrNull() == null) {
                return "Не все нужные игроки выбрали карту для тумана"
            }
        }
        return null
    }

    override fun play(move: Move, round: Round) {
        move.initiator.playCard(this)
        if (round.getCurrentTurn() >= 4) return

        val otherPlayers = round.getPlayers().filter { it != move.initiator }
        val fogPool = mutableListOf<Card>()

        otherPlayers.forEach { player -> // сбор карт для тумана
            val cardToGive = move.additionalData?.get(player)?.firstOrNull()
            if (cardToGive != null) {
                player.removeCard(cardToGive)
                fogPool.add(cardToGive)
            }
        }

        otherPlayers.forEach { player ->
            player.addCard(fogPool.random())
        }
    }
}

object TradeCard : Card() {
    override val title: String = "Обмен"
    override val description: String = "Обменяйтесь 1 или 2 картами с выбранным игроком. Не работает на 4 ходу."

    override fun validate(move: Move, round: Round): String? {
        if (round.getCurrentTurn() >= 4) return null // 4 ход
        if (move.target == null) return "Нужно выбрать игрока для обмена"
        if (move.target == move.initiator) return "Нельзя использовать на себя"

        val initiatorCardsToGive = move.additionalData?.get(move.initiator) ?: emptyList()
        val targetCardsToGive = move.additionalData?.get(move.target) ?: emptyList()

        val tradeCount = initiatorCardsToGive.size

        if (tradeCount != 1 && tradeCount != 2) {
            return "Вы должны выбрать 1 или 2 карты для обмена."
        }

        if (initiatorCardsToGive.size != targetCardsToGive.size) {
            return "Количество карт для обмена не совпадает"
        }

        if (move.target.getCards().size < tradeCount) {
            return "Неверное количество карт для обмена"
        }

        if (!move.initiator.getCards().containsAll(initiatorCardsToGive)) {
            return "Вы пытаетесь отдать карту, которой у вас нет"
        }
        if (!move.target.getCards().containsAll(targetCardsToGive)) {
            return "Цель пытается отдать карту, которой у неё нет"
        }

        return null
    }

    override fun play(move: Move, round: Round) {
        move.initiator.playCard(this)
        if (round.getCurrentTurn() >= 4) return

        val target = move.target!!
        val initiatorCards = move.additionalData?.get(move.initiator) ?: emptyList()
        val targetCards = move.additionalData?.get(target) ?: emptyList()

        initiatorCards.forEach { card ->
            move.initiator.removeCard(card)
            target.addCard(card)
        }

        targetCards.forEach { card ->
            target.removeCard(card)
            move.initiator.addCard(card)
        }
    }
}

object HoundCard : Card() {
    override val title: String = "Пёс"
    override val description: String = "Цель сбрасывает выбранную карту, а Пёс переходит к ней в руку"

    override fun validate(move: Move, round: Round): String? {
        if (move.target == null) return "Выберите цель"

        val targetCard = move.additionalData?.get(move.target)
        if (targetCard == null && move.target.getCards().isNotEmpty()) {
            return "Цель должна выбрать карту для сброса"
        }
        return null
    }

    override fun play(move: Move, round: Round) {
        move.initiator.removeCard(this) // удаляем, но не играем

        val target = move.target!!
        val cardToDrop = move.additionalData?.get(target)?.firstOrNull()

        if (cardToDrop != null) {
            target.discardCard(cardToDrop)

            if (cardToDrop is Monster) {
                round.finishWithHuntersWin()
            }
        }
        target.addCard(this)
    }
}

