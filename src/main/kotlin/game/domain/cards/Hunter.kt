package game.domain.cards

import game.domain.Card
import game.domain.CardEffect
import game.domain.CardRole

abstract class Hunter : Card() {
    override val winPoints: Int = 2
    final override val role: CardRole = CardRole.HUNTER
}

object HunterCard : Hunter() {
    override val title: String = "Охотник"
    override val description: String = "Цель сбрасывает монстра. Нет эффекта на 1-м ходу."
    override val effect: CardEffect = CardEffect.HUNTER
    override val requiresTarget: Boolean = true
}
