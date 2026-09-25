package game.domain.cards

import game.domain.Card
import game.domain.CardEffect
import game.domain.CardRole

abstract class Hide : Card() {
    final override val role: CardRole = CardRole.HIDE
}

object HideCard : Hide() {
    override val title: String = "Укрытие"
    override val description: String = "Пока эта карта у вас в руке, игнорируйте эффект карт охотников"
    override val effect: CardEffect = CardEffect.HIDE
}
