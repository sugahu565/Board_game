package game.domain.cards

import game.domain.Card
import game.domain.Move
import game.domain.Round

abstract class Friend: Card() {
    override val winPoints: Int = 1
}

object FriendCard : Friend() {
    override val title: String = "Друг"
    override val description: String = "Выбранный игрок сбрасывает Охотника"

    override fun validate(move: Move, round: Round): String? {
        if (move.target == null) return "Нужно выбрать игрока"
        return null
    }

    override fun play(move: Move, round: Round) {
        move.initiator.playCard(this)

        val targetPlayer = move.target!!
        val hunter = targetPlayer.getCards().firstOrNull { it is Hunter }

        if (hunter != null) {
            targetPlayer.discardCard(hunter) // сброс
            targetPlayer.addCard(round.deck.nextCard()) // передача новой карты
        }
    }
}