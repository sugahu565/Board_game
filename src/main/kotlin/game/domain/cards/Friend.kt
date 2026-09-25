package game.domain.cards

import game.domain.Card
import game.domain.CardEffect
import game.domain.CardRole

abstract class Friend: Card() {
    override val winPoints: Int = 1
    final override val role: CardRole = CardRole.FRIEND
}

object FriendCard : Friend() {
    override val title: String = "Друг"
    override val description: String = "Выбранный игрок сбрасывает Охотника"
    override val effect: CardEffect = CardEffect.FRIEND
    override val requiresTarget: Boolean = true
}
