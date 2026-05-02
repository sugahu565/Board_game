package game.domain.cards

import game.domain.Card
import game.domain.Move
import game.domain.Round

abstract class Hide : Card()

object HideCard : Hide() {
    override val title: String = "Укрытие"
    override val description: String = "Пока эта карта у вас в руке, игнорируйте эффект карт охотников"

    override fun validate(move: Move, round: Round): String? {
        return null
    }

    override fun play(move: Move, round: Round) {
        move.initiator.playCard(this)
    }
}