package game.domain

import game.domain.cards.Monster

interface CardEffectHandler {
    fun validate(move: Move, round: Round): String?
    fun apply(move: Move, round: Round): MoveResult
}

class DefaultCardEffectHandler : CardEffectHandler {
    override fun validate(move: Move, round: Round): String? {
        val target = move.target
        if (move.playerCard.requiresTarget && target == null)
            return "Необходимо выбрать цель"

        if (move.playerCard.requiresTarget && target == move.initiator &&
            move.playerCard.effect != CardEffect.FRIEND && move.playerCard.effect != CardEffect.HOUND)
            return "Нельзя использовать карту на себя"

        if (round.getCurrentTurn() >= 4 && move.playerCard.effect in fourthTurnDisabledEffects)
            return null

        return when (move.playerCard.effect) {
            CardEffect.THEFT -> validateTheft(move)
            CardEffect.TRADE -> validateTrade(move)
            CardEffect.FOG -> validateChoices(move, round, includeInitiator = false)
            CardEffect.GOSSIP -> validateChoices(move, round, includeInitiator = true)
            CardEffect.HOUND -> validateHound(move)
            CardEffect.VAMPIRE, CardEffect.KUMIHO, CardEffect.MERMAID ->
                if (round.getCurrentTurn() < 4) "Этого монстра нельзя сыграть раньше 4 хода" else null
            CardEffect.WEREWOLF -> validateWerewolf(round)
            CardEffect.MANDRAGORA -> validateMandragora(round)
            else -> null
        }
    }

    override fun apply(move: Move, round: Round): MoveResult {
        if (move.playerCard.effect == CardEffect.HOUND)
            return applyHound(move)

        move.initiator.playCard(move.playerCard)
        if (round.getCurrentTurn() >= 4 && move.playerCard.effect in fourthTurnDisabledEffects)
            return applied()

        return when (move.playerCard.effect) {
            CardEffect.RUMORS -> applied(applyRumors(round))
            CardEffect.THEFT -> applied(applyTheft(move))
            CardEffect.INVESTIGATION -> applied(MoveEvent.CardsRevealed(move.target!!, move.target.getCards()))
            CardEffect.GOSSIP -> applied(applyGossip(move, round))
            CardEffect.FOG -> applied(applyFog(move))
            CardEffect.TRADE -> applied(applyTrade(move))
            CardEffect.FRIEND -> applied(applyFriend(move, round))
            CardEffect.HUNTER -> applyHunter(move, round)
            CardEffect.VAMPIRE, CardEffect.KUMIHO, CardEffect.WEREWOLF,
            CardEffect.MANDRAGORA, CardEffect.MERMAID -> MoveResult.Applied(RoundStatus.MONSTER_WON)
            else -> applied()
        }
    }

    private fun applied(event: MoveEvent? = null): MoveResult.Applied =
        MoveResult.Applied(RoundStatus.IN_PROGRESS, event)

    private fun validateTheft(move: Move): String? {
        val details = move.details as? MoveDetails.Theft ?: return "Выберите карту, которую отдадите цели"
        val card = details.cardToGive ?: return "Выберите карту, которую отдадите цели"
        return if (card !in move.initiator.getCards() || card == move.playerCard) "Выбранной карты нет в руке" else null
    }

    private fun validateTrade(move: Move): String? {
        val details = move.details as? MoveDetails.Trade ?: return "Выберите карты для обмена"
        if (details.initiatorCards.size !in 1..2) return "Выберите 1 или 2 карты для обмена"
        if (details.initiatorCards.size != details.targetCards.size) return "Количество карт для обмена не совпадает"
        if (move.playerCard in details.initiatorCards || !ownsAll(move.initiator, details.initiatorCards)) return "Инициатор пытается отдать карту, которой у него нет"
        if (!ownsAll(move.target!!, details.targetCards)) return "Цель пытается отдать карту, которой у неё нет"
        return null
    }

    private fun validateChoices(move: Move, round: Round, includeInitiator: Boolean): String? {
        val choices = when (val details = move.details) {
            is MoveDetails.Fog -> details.choices
            is MoveDetails.Gossip -> details.choices
            else -> return "Не все игроки выбрали карты"
        }
        val required = round.getPlayers().filter { includeInitiator || it != move.initiator }.filter { it.getCards().isNotEmpty() }
        val valid = choices.size == required.size && required.all { player ->
            choices.count { it.player == player && it.card in player.getCards() } == 1
        }
        val playedCardSelected = choices.any { it.player == move.initiator && it.card == move.playerCard }
        return if (valid && !playedCardSelected) null
        else "Не все игроки выбрали карты"
    }

    private fun validateHound(move: Move): String? {
        val details = move.details as? MoveDetails.Hound ?: return "Цель должна выбрать карту для сброса"
        val card = details.cardToDiscard ?: return "Цель должна выбрать карту для сброса"
        return if (card in move.target!!.getCards()) null else "У цели нет выбранной карты"
    }

    private fun validateWerewolf(round: Round): String? = when {
        round.getCurrentTurn() >= 4 -> null
        round.getCurrentTurn() < 2 -> "Оборотня нельзя сыграть на 1 ходе"
        round.countPlayedCards { it.role == CardRole.HUNTER } >= 2 -> null
        else -> "Для Оборотня нужно два разыгранных Охотника"
    }

    private fun validateMandragora(round: Round): String? = when {
        round.getCurrentTurn() >= 4 -> null
        round.getCurrentTurn() < 2 -> "Мандрагору нельзя сыграть на 1 ходе"
        round.countPlayedCards { it.effect == CardEffect.VILLAGER } >= 2 -> null
        else -> "Для Мандрагоры нужно два разыгранных мирных жителя"
    }

    private fun applyRumors(round: Round): MoveEvent? {
        val players = round.getPlayers()
        val cards = players.map { it to it.getCards().randomOrNull() }
        cards.forEach { (player, card) -> if (card != null) player.removeCard(card) }
        cards.forEachIndexed { index, (_, card) -> if (card != null) players[(index + 1) % players.size].addCard(card) }
        return null
    }

    private fun applyTheft(move: Move): MoveEvent? {
        val target = move.target!!
        target.getCards().randomOrNull()?.let { target.removeCard(it); move.initiator.addCard(it) }
        (move.details as MoveDetails.Theft).cardToGive?.let { move.initiator.removeCard(it); target.addCard(it) }
        return null
    }

    private fun applyGossip(move: Move, round: Round): MoveEvent? {
        val players = round.getPlayers()
        val choices = (move.details as MoveDetails.Gossip).choices.associate { it.player to it.card }
        choices.forEach { (player, card) -> player.removeCard(card) }
        players.forEachIndexed { index, player -> choices[player]?.let { players[(index - 1 + players.size) % players.size].addCard(it) } }
        return null
    }

    private fun applyFog(move: Move): MoveEvent? {
        val choices = (move.details as MoveDetails.Fog).choices
        val cards = choices.map { it.card }.shuffled().toMutableList()
        choices.forEach { it.player.removeCard(it.card) }
        choices.forEach { choice -> if (cards.isNotEmpty()) choice.player.addCard(cards.removeAt(0)) }
        return null
    }

    private fun applyTrade(move: Move): MoveEvent? {
        val target = move.target!!
        val details = move.details as MoveDetails.Trade
        details.initiatorCards.forEach(move.initiator::removeCard)
        details.targetCards.forEach(target::removeCard)
        details.initiatorCards.forEach(target::addCard)
        details.targetCards.forEach(move.initiator::addCard)
        return null
    }

    private fun applyFriend(move: Move, round: Round): MoveEvent? {
        val target = move.target!!
        target.getCards().firstOrNull { it.role == CardRole.HUNTER }?.let {
            target.discardCard(it)
            if (!round.deck.isEmpty()) target.addCard(round.deck.nextCard())
        }
        return null
    }

    private fun applyHunter(move: Move, round: Round): MoveResult.Applied {
        val target = move.target!!
        if (round.getCurrentTurn() == 1 || target.hasHideCard()) return applied()
        val monster = target.getCards().filterIsInstance<Monster>().firstOrNull() ?: return applied()
        if (monster.canBeKilledByHunter(round.getCurrentTurn(), round.countPlayedCards { it.role == CardRole.HUNTER })) {
            target.discardCard(monster)
            return MoveResult.Applied(RoundStatus.HUNTERS_WON)
        }
        return applied()
    }

    private fun applyHound(move: Move): MoveResult.Applied {
        move.initiator.removeCard(move.playerCard)
        val target = move.target!!
        var roundStatus = RoundStatus.IN_PROGRESS
        (move.details as MoveDetails.Hound).cardToDiscard?.let {
            target.discardCard(it)
            if (it.role == CardRole.MONSTER) roundStatus = RoundStatus.HUNTERS_WON
        }
        target.addCard(move.playerCard)
        return MoveResult.Applied(roundStatus)
    }

    private fun ownsAll(player: Player, cards: List<Card>): Boolean {
        val hand = player.getCards()
        return cards.groupingBy { it }.eachCount().all { (card, count) -> hand.count { it == card } >= count }
    }

    private companion object {
        val fourthTurnDisabledEffects = setOf(CardEffect.RUMORS, CardEffect.THEFT, CardEffect.GOSSIP, CardEffect.FOG, CardEffect.TRADE)
    }
}
